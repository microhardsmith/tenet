#include <errno.h>
#include <WinSock2.h>
#include <WS2tcpip.h>
#include <stdio.h>
#include "wepoll.h"
#include "lib_win.h"

#pragma comment (lib, "ws2_32.lib")

static inline void clean(void* handle) {
    int close = epoll_close(handle);
    if(close == -1) {
        printf("Error at close epoll : %d \n", w_get_last_error());
    }
    int clean = w_clean_up();
    if(clean == -1) {
        printf("Error at cleaning up : %d \n", w_get_last_error());
    }
    exit(-1);
}

// only for test purpose
int main() {
    void* handle = w_epoll_create();
    if(handle == NULL) {
        printf("Can't create wepoll \n");
        return -1;
    }
    SOCKET socket = w_socket_create();
    if(socket == -1) {
        printf("Error at creating socket : %d \n", w_get_last_error());
        clean(handle);
    }
    printf("Creating socket : %llu \n", socket);

    // 设置reuse addr
    int reuseAddrResult = w_set_reuse_addr(socket, 1);
    if(reuseAddrResult == -1) {
        printf("Error at set reuse addr : %d \n", w_get_last_error());
        clean(handle);
    }
    // 设置keepalive
    int keepAliveResult = w_set_keep_alive(socket, 0);
    if(keepAliveResult == -1) {
        printf("Error at set keep alive : %d \n", w_get_last_error());
        clean(handle);
    }
    // 设置tcp nodelay
    int noDelayResult = w_set_tcp_no_delay(socket, 0);
    if(noDelayResult == -1) {
        printf("Error at set no delay result : %d \n", w_get_last_error());
        clean(handle);
    }

    // 设置为非阻塞
    long long arg = 1;
    int nonBlockingResult = w_set_nonblocking(socket);
    if(nonBlockingResult == -1) {
        printf("Error at set NonBlocking : %d \n", w_get_last_error());
        clean(handle);
    }
    // 绑定
    struct sockaddr_in serverAddr;
    char loc[] = "127.0.0.1";
    u_short serverPort = 10705;
    int setSockAddrResult = w_set_sock_addr(&serverAddr, loc, serverPort);
    if(setSockAddrResult != 1) {
        printf("set sock addr result : %d \n", setSockAddrResult);
        printf("Error at set socket addr : %d \n", w_get_last_error());
        clean(handle);
    }
    printf("sin family : %hu", serverAddr.sin_family);
    int bindResult = w_bind(socket, &serverAddr, sizeof(serverAddr));
    if(bindResult == -1) {
        printf("Error at binding socket : %d \n", w_get_last_error());
        clean(handle);
    }
    // 监听,backlog size设置为128
    int listenResult = w_listen(socket, 128);
    if(listenResult == -1) {
        printf("Error at listening socket : %d \n", w_get_last_error());
        clean(handle);
    }
    
    struct epoll_event ev, events[20];
    ev.events = EPOLLIN;
    ev.data.fd = socket;
    int epollCtlResult = w_epoll_ctl(handle, EPOLL_CTL_ADD, socket, &ev);
    if(epollCtlResult == -1) {
        printf("Error at epoll ctl : %d \n", w_get_last_error());
        clean(handle);
    }

    struct sockaddr_in clientAddr;
    int len = w_address_len();
    char* addrStr = (char*) malloc(len * sizeof(char));
    memset(addrStr, 0, len);
    u_short p;
    while(TRUE) {
        int size = w_epoll_wait(handle, events, 20, -1);
        for(int i = 0; i < size; i++) {
            struct epoll_event event = events[i];
            if(event.data.fd == socket) {
                int client = w_accept(socket, &clientAddr, sizeof(clientAddr));
                if(client == -1) {
                    printf("Error at accept : %d \n", w_get_last_error());
                }
                int addressResult = w_address(&clientAddr, addrStr, len);
                if(addressResult == -1) {
                    printf("Parsing client address failed : %d \n", w_get_last_error());
                }
                p = w_port(&clientAddr);
                printf("Receiving connection from remote : %s:%d \n", addrStr, p);
                if(w_set_nonblocking(socket) == -1) {
                    printf("Err set nonBlocking \n");
                }
                ev.data.fd = client;
                if(w_epoll_ctl(handle, EPOLL_CTL_ADD, client, &ev) == -1) {
                    printf("Err epoll ctl \n");
                }
            
            }else {
                printf("handle event : %d \n", event.events);
                if(ev.events == EPOLLIN) {
                    int fd = event.data.fd;
                    char buf[10];
                    memset(buf, 0, sizeof(buf));
                    int n = w_recv(fd, buf, 10);
                    if(n < 0) {
                        printf("read error \n");
                    }else if(n == 0) {
                        printf("close socket : %d \n", fd);
                        if(w_epoll_ctl(handle, EPOLL_CTL_DEL, fd, NULL) == -1) {
                            printf("Err epoll ctl \n");
                        }
                        if(w_close_socket(fd) == -1) {
                            printf("close err : %d \n", w_get_last_error());
                        }
                    }else {
                        printf("read bytes : %d \n", n);
                        printf("receive msg : %s \n", buf);
                    }
                }
            }
        }
    }
    free(addrStr);
    addrStr = NULL;
    if(w_close_socket(socket) == -1) {
        printf("close socket err : %d \n", w_get_last_error());
    }
    int close = w_epoll_close(handle);
    if(close == -1) {
        printf("Error at close epoll : %d \n", w_get_last_error());
        return -1;
    }
    int clean = w_clean_up();
    if(clean == SOCKET_ERROR) {
        printf("Error at cleaning up : %d \n", w_get_last_error());
        return -1;
    }
    return 0;
}

// 获取标准输入流
FILE* g_stdout() {
    return stdout;
}

// 获取标准错误流
FILE* g_stderr() {
    return stderr;
}

// 输出字符串至指定流
void g_print(char* str, FILE* stream) {
    fputs(str, stream);
    fflush(stream);
}


// 返回connect导致阻塞的错误码
int w_connect_block_code() {
    return WSAEWOULDBLOCK;
}

// 返回send导致阻塞的错误码
int w_send_block_code() {
    return WSAEWOULDBLOCK;
}

// 返回无效socket值
SOCKET w_invalid_socket() {
    return INVALID_SOCKET;
}

// 获取IP地址字符串长度
int w_address_len() {
    return INET_ADDRSTRLEN;
}

// 创建epoll,返回创建的epoll句柄
void* w_epoll_create() {
    // the size parameter means noting now, must be positive
    return epoll_create(1);
}

// epoll_ctl 操控事件,失败则返回-1,成功则返回0
int w_epoll_ctl(void* handle, int op, SOCKET socket, struct epoll_event* event) {
    return epoll_ctl(handle, op, socket, event);
}

// epoll_wait,出现错误则返回-1,否则返回触发的事件个数
int w_epoll_wait(void* handle, struct epoll_event* events, int maxevents, int timeout) {
    return epoll_wait(handle, events, maxevents, timeout);
}

// epoll_close,出现错误则返回-1,否则返回0
int w_epoll_close(void* handle) {
    return epoll_close(handle);
}

// 获取客户端连接的地址,写入到指定addrStr下出现错误则返回-1,否则返回0
int w_address(struct sockaddr_in* clientAddr, char* addrStr, int len) {
    const char* result = inet_ntop(AF_INET, clientAddr, addrStr, len);
    if(result == NULL) {
        return -1;
    }
    return 0;
}

// 获取客户端连接的端口号
u_short w_port(struct sockaddr_in* clientAddr) {
    return ntohs(clientAddr -> sin_port);
}

// 创建socket,失败则返回-1,成功则返回socket值
SOCKET w_socket_create() {
    return socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
}

// accept 连接,出现错误则返回-1,否则返回建立的fd
SOCKET w_accept(SOCKET socket, struct sockaddr_in* clientAddr, int clientAddrSize) {
    return accept(socket, (struct sockaddr *) clientAddr, &clientAddrSize);
}

// 设置sock addr,失败则返回-1,如果address字符串不合法会返回0,否则返回1
int w_set_sock_addr(struct sockaddr_in* sockAddr, char* address, u_short port) {
    sockAddr -> sin_family = AF_INET;  //使用IPv4地址
    sockAddr -> sin_port = htons(port);  //设置端口
    return inet_pton(AF_INET, address == NULL ?  INADDR_ANY : address, &(sockAddr -> sin_addr)); //设置IP地址
}

// 设置TCP_NODELAY为指定值,失败返回-1,成功返回0
int w_set_reuse_addr(SOCKET socket, int value) {
    void* ptr = &value;
    int result = setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, ptr, sizeof(value));
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 设置TCP_NODELAY为指定值,失败返回-1,成功返回0
int w_set_keep_alive(SOCKET socket, int value) {
    return setsockopt(socket, SOL_SOCKET, SO_KEEPALIVE, (char*) &value, sizeof(value));
}

// 设置TCP_NODELAY为指定值,失败返回-1,成功返回0
int w_set_tcp_no_delay(SOCKET socket, int value) {
    return setsockopt(socket, IPPROTO_TCP, TCP_NODELAY, (char*) &value, sizeof(value));
}

// 获取指定socket上的错误码,如果socket上无错误应返回0
int w_get_err_opt(SOCKET socket, int* ptr) {
    int ptr_size = sizeof(int);
    return getsockopt(socket, SOL_SOCKET, SO_ERROR, (char*) ptr, &ptr_size);
}

// 设置指定socket为非阻塞,失败返回-1,成功返回0
int w_set_nonblocking(SOCKET socket) {
    u_long argp = 1;
    return ioctlsocket(socket, FIONBIO, &argp);
}

// 绑定socket到固定端口,失败则返回-1,成功则返回0
int w_bind(SOCKET socket, struct sockaddr_in* sockAddr, int size) {
    return bind(socket, (SOCKADDR*) sockAddr, size);
}

// 客户端建立连接,失败则返回-1,成功则返回0
int w_connect(SOCKET socket, struct sockaddr_in* sockAddr, int size) {
    return connect(socket, (SOCKADDR*) sockAddr, size);
}

// 设置socket监听,失败则返回-1,成功则返回0
int w_listen(SOCKET socket, int backlog) {
    return listen(socket, SOMAXCONN_HINT(backlog));
}

// 从socket接受数据,失败则返回-1,成功则返回已接收字节数,远端已关闭则返回0
int w_recv(SOCKET socket, char* buf, int len) {
    return recv(socket, buf, len, 0);
}

// 向socket发送数据,失败则返回-1,成功则返回已接收字节数
int w_send(SOCKET socket, char* buf, int len) {
    return send(socket, buf, len, 0);
}

// 关闭socket连接,失败则返回-1,成功则返回0
int w_close_socket(SOCKET socket) {
    return closesocket(socket);
}

// 关闭socket写端,失败则返回-1,成功则返回0
int w_shutdown_write(SOCKET socket) {
    return shutdown(socket, SD_SEND);
}

// 获取上一个WSA错误码
int w_get_last_error() {
    return WSAGetLastError();
}

// 清理当前wsa使用,失败则返回-1,成功则返回0
int w_clean_up() {
    return WSACleanup();
}
