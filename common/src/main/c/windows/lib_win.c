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
        printf("Error at close epoll : %d \n", wsa_get_last_error());
    }
    int clean = wsa_clean_up();
    if(clean == -1) {
        printf("Error at cleaning up : %d \n", wsa_get_last_error());
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
        printf("Error at creating socket : %d \n", wsa_get_last_error());
        clean(handle);
    }
    printf("Creating socket : %llu \n", socket);

    boolean yes = TRUE, no = FALSE;
    // 设置reuse addr
    int reuseAddrResult = w_set_reuse_addr(socket, TRUE);
    if(reuseAddrResult == -1) {
        printf("Error at set reuse addr : %d \n", wsa_get_last_error());
        clean(handle);
    }
    // 设置keepalive
    int keepAliveResult = w_set_keep_alive(socket, FALSE);
    if(keepAliveResult == -1) {
        printf("Error at set keep alive : %d \n", wsa_get_last_error());
        clean(handle);
    }
    // 设置tcp nodelay
    int noDelayResult = w_set_tcp_no_delay(socket, FALSE);
    if(noDelayResult == -1) {
        printf("Error at set no delay result : %d \n", wsa_get_last_error());
        clean(handle);
    }

    // 设置为非阻塞
    long long arg = 1;
    int nonBlockingResult = w_set_nonblocking(socket);
    if(nonBlockingResult == -1) {
        printf("Error at set NonBlocking : %d \n", wsa_get_last_error());
        clean(handle);
    }
    // 绑定
    struct sockaddr_in serverAddr;
    char loc[] = "127.0.0.1";
    int serverPort = 10705;
    int setSockAddrResult = w_set_sock_addr(&serverAddr, loc, serverPort);
    if(setSockAddrResult != 1) {
        printf("set sock addr result : %d \n", setSockAddrResult);
        printf("Error at set socket addr : %d \n", wsa_get_last_error());
        clean(handle);
    }
    printf("sin family : %hu", serverAddr.sin_family);
    int bindResult = w_bind(&serverAddr, socket, sizeof(serverAddr));
    if(bindResult == -1) {
        printf("Error at binding socket : %d \n", wsa_get_last_error());
        clean(handle);
    }
    // 监听,backlog size设置为128
    int listenResult = w_listen(socket, 128);
    if(listenResult == -1) {
        printf("Error at listening socket : %d \n", wsa_get_last_error());
        clean(handle);
    }
    
    struct epoll_event ev, events[20];
    ev.events = EPOLLIN;
    ev.data.fd = socket;
    int epollCtlResult = w_epoll_ctl_add(handle, socket, &ev);
    if(epollCtlResult == -1) {
        printf("Error at epoll ctl : %d \n", wsa_get_last_error());
        clean(handle);
    }

    struct sockaddr_in clientAddr;
    int len = w_address_len();
    char* addrStr = (char*) malloc(len * sizeof(char));
    memset(addrStr, 0, len);
    int p;
    while(TRUE) {
        int size = w_epoll_wait(handle, events, 20, -1);
        for(int i = 0; i < size; i++) {
            struct epoll_event event = events[i];
            if(event.data.fd == socket) {
                int client = w_accept(socket, &clientAddr, sizeof(clientAddr));
                if(client == -1) {
                    printf("Error at accept : %d \n", wsa_get_last_error());
                }
                int addressResult = w_address(&clientAddr, addrStr, len);
                if(addressResult == -1) {
                    printf("Parsing client address failed : %d \n", wsa_get_last_error());
                }
                p = w_port(&clientAddr);
                printf("Receiving connection from remote : %s:%d \n", addrStr, p);
                if(w_set_nonblocking(socket) == -1) {
                    printf("Err set nonBlocking \n");
                }
                ev.data.fd = client;
                if(w_epoll_ctl_add(handle, client, &ev) == -1) {
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
                        if(w_epoll_ctl_del(handle, fd) == -1) {
                            printf("Err epoll ctl \n");
                        }
                        if(w_close_socket(fd) == -1) {
                            printf("close err : %d \n", wsa_get_last_error());
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
        printf("close socket err : %d \n", wsa_get_last_error());
    }
    int close = w_epoll_close(handle);
    if(close == -1) {
        printf("Error at close epoll : %d \n", wsa_get_last_error());
        return -1;
    }
    int clean = wsa_clean_up();
    if(clean == SOCKET_ERROR) {
        printf("Error at cleaning up : %d \n", wsa_get_last_error());
        return -1;
    }
    return 0;
}

// 向标准输出流输出字符
void g_puts(char* str) {
    puts(str);
}

// 向标准输出流刷新缓冲区
void g_flush() {
    fflush(stdout);
}

// 创建epoll,返回创建的epoll句柄
void* w_epoll_create() {
    // the size parameter means noting now, must be positive
    return epoll_create(1);
}

// epoll_ctl 添加事件,失败则返回-1,成功则返回0
int w_epoll_ctl_add(void* handle, SOCKET socket, struct epoll_event* event) {
    return epoll_ctl(handle, EPOLL_CTL_ADD, socket, event);
}

// epoll_ctl 删除事件,失败则返回-1,成功则返回0
int w_epoll_ctl_del(void* handle, SOCKET socket) {
    return epoll_ctl(handle, EPOLL_CTL_DEL, socket, NULL);
}

// epoll_wait,出现错误则返回-1,否则返回触发的事件个数
int w_epoll_wait(void* handle, struct epoll_event* events, int maxevents, int timeout) {
    return epoll_wait(handle, events, maxevents, timeout);
}

// epoll_close,出现错误则返回-1,否则返回0
int w_epoll_close(void* handle) {
    return epoll_close(handle);
}

// 获取IP地址字符串长度
int w_address_len() {
    return INET_ADDRSTRLEN;
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
int w_port(struct sockaddr_in* clientAddr) {
    return ntohs(clientAddr -> sin_port);
}

// 创建socket,失败则返回-1,成功则返回socket值
SOCKET w_socket_create() {
    SOCKET servSock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if(servSock == INVALID_SOCKET) {
        return -1;
    }else {
        return servSock;
    }
}

// accept 连接,出现错误则返回-1,否则返回建立的fd
int w_accept(SOCKET socket, struct sockaddr_in* clientAddr, int clientAddrSize) {
    int result = accept(socket, (struct sockaddr *) clientAddr, &clientAddrSize);
    if(result == INVALID_SOCKET) {
        return -1;
    }
    return result;
}


// 设置sock addr,失败则返回-1,如果address字符串不合法会返回0,否则返回1
int w_set_sock_addr(struct sockaddr_in* sockAddr, char* address, int port) {
    memset(sockAddr, 0, sizeof(struct sockaddr_in));  //每个字节都用0填充
    sockAddr -> sin_family = AF_INET;  //使用IPv4地址
    sockAddr -> sin_port = htons(port);  //设置端口
    return inet_pton(AF_INET, address == NULL ?  INADDR_ANY : address, &(sockAddr -> sin_addr)); //设置IP地址
}

// 设置TCP_NODELAY为指定值,失败返回-1,成功返回0
int w_set_reuse_addr(SOCKET socket, boolean value) {
    void* ptr = &value;
    int result = setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, ptr, sizeof(value));
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 设置TCP_NODELAY为指定值,失败返回-1,成功返回0
int w_set_keep_alive(SOCKET socket, boolean value) {
    void* ptr = &value;
    int result = setsockopt(socket, SOL_SOCKET, SO_KEEPALIVE, ptr, sizeof(value));
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 设置TCP_NODELAY为指定值,失败返回-1,成功返回0
int w_set_tcp_no_delay(SOCKET socket, boolean value) {
    void* ptr = &value;
    int result = setsockopt(socket, IPPROTO_TCP, TCP_NODELAY, ptr, sizeof(value));
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 设置指定socket为非阻塞,失败返回-1,成功返回0
int w_set_nonblocking(SOCKET socket) {
    u_long argp = 1;
    int result = ioctlsocket(socket, FIONBIO, &argp);
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 绑定socket到固定端口,失败则返回-1,成功则返回0
int w_bind(struct sockaddr_in* sockAddr, SOCKET socket, int size) {
    int result = bind(socket, (SOCKADDR*) sockAddr, size);
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 设置socket监听,失败则返回-1,成功则返回0
int w_listen(SOCKET socket, int backlog) {
    int result = listen(socket, SOMAXCONN_HINT(128));
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 从socket接受数据,失败则返回-1,成功则返回已接收字节数,远端已关闭则返回0
int w_recv(SOCKET socket, char* buf, int len) {
    int result = recv(socket, buf, len, 0);
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 关闭socket连接，失败则返回-1,成功则返回0
int w_close_socket(SOCKET socket) {
    int result = closesocket(socket);
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}


// 获取上一个WSA错误码
int wsa_get_last_error() {
    return WSAGetLastError();
}

// 清理当前wsa使用,失败则返回-1,成功则返回0
int wsa_clean_up() {
    int result = WSACleanup();
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}
