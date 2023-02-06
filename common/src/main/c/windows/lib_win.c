

#include <errno.h>
#include <WinSock2.h>
#include <WS2tcpip.h>
#include <stdio.h>
#include "wepoll.h"
#include "lib_win.h"

#pragma comment (lib, "ws2_32.lib")

static inline void clean(void* handle) {
    int close = tenet_epoll_close(handle);
    if(close == -1) {
        printf("Error at close epoll : %d \n", tenet_wsa_get_last_error());
    }
    int clean = tenet_wsa_clean_up();
    if(clean == SOCKET_ERROR) {
        printf("Error at cleaning up : %d \n", tenet_wsa_get_last_error());
    }
    exit(-1);
}

// only for test purpose
int main() {
    void* handle = tenet_epoll_create();
    if(handle == NULL) {
        printf("Can't create wepoll \n");
        return -1;
    }
    int socket = tenet_socket_create();
    if(socket == -1) {
        printf("Error at creating socket : %d \n", tenet_wsa_get_last_error());
        clean(handle);
    }
    printf("Creating socket : %d \n", socket);

    boolean yes = TRUE, no = FALSE;
    // 设置reuse addr
    int reuseAddrResult = tenet_set_socket_opt(socket, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(yes));
    if(reuseAddrResult == -1) {
        printf("Error at set reuse addr : %d \n", tenet_wsa_get_last_error());
        clean(handle);
    }
    // 设置keepalive
    int keepAliveResult = tenet_set_socket_opt(socket, SOL_SOCKET, SO_KEEPALIVE, &no, sizeof(no));
    if(keepAliveResult == -1) {
        printf("Error at set keep alive : %d \n", tenet_wsa_get_last_error());
        clean(handle);
    }
    // 设置tcp nodelay
    int noDelayResult = tenet_set_socket_opt(socket, IPPROTO_TCP, TCP_NODELAY, &no, sizeof(no));
    if(noDelayResult == -1) {
        printf("Error at set no delay result : %d \n", tenet_wsa_get_last_error());
        clean(handle);
    }

    // 设置为非阻塞
    u_long argp = 1;
    int nonBlockingResult = tenet_ioctl_socket(socket, FIONBIO, &argp);
    if(nonBlockingResult == -1) {
        printf("Error at set NonBlocking : %d \n", tenet_wsa_get_last_error());
        clean(handle);
    }
    // 绑定
    struct sockaddr_in serverAddr;
    char loc[] = "127.0.0.1";
    int serverPort = 19999;
    int setSockAddrResult = tenet_set_sockaddr(&serverAddr, loc, serverPort);
    if(setSockAddrResult != 1) {
        printf("set sock addr result : %d \n", setSockAddrResult);
        printf("Error at set socket addr : %d \n", tenet_wsa_get_last_error());
        clean(handle);
    }
    int bindResult = tenet_bind(&serverAddr, socket);
    if(bindResult == -1) {
        printf("Error at binding socket : %d \n", tenet_wsa_get_last_error());
        clean(handle);
    }
    // 监听,backlog size设置为128
    int listenResult = tenet_listen(socket, 128);
    if(listenResult == -1) {
        printf("Error at listening socket : %d \n", tenet_wsa_get_last_error());
        clean(handle);
    }
    
    struct epoll_event ev, events[20];
    ev.events = EPOLLIN;
    ev.data.fd = socket;
    int epollCtlResult = tenet_epoll_ctl(handle, EPOLL_CTL_ADD, socket, &ev);
    if(epollCtlResult == -1) {
        printf("Error at epoll ctl : %d \n", tenet_wsa_get_last_error());
        clean(handle);
    }

    struct sockaddr_in clientAddr;
    int clientAddrSize = sizeof(clientAddr);
    char addrStr[INET_ADDRSTRLEN];
    int port;
    while(TRUE) {
        int size = tenet_epoll_wait(handle, events, 20, -1);
        for(int i = 0; i < size; i++) {
            struct epoll_event event = events[i];
            if(event.data.fd == socket) {
                int client = tenet_accept(socket, &clientAddr, &clientAddrSize, addrStr, &port);
                if(client == -1) {
                    printf("Error at accept : %d \n", tenet_wsa_get_last_error());
                }
                printf("Receiving connection from remote : %s:%d \n", addrStr, port);
                if(tenet_ioctl_socket(socket, FIONBIO, &argp) == -1) {
                    printf("Err set nonBlocking \n");
                }
                ev.data.fd = client;
                if(tenet_epoll_ctl(handle, EPOLL_CTL_ADD, client, &ev) == -1) {
                    printf("Err epoll ctl \n");
                }
            
            }else {
                printf("handle event : %d \n", event.events);
                if(ev.events == EPOLLIN) {
                    int fd = event.data.fd;
                    char buf[10];
                    memset(buf, 0, sizeof(buf));
                    int n = 0;
                    n = recv(fd, buf, 10, 0);
                    printf("read bytes : %d \n", n);
                    if(n < 0) {
                        printf("read error \n");
                    }else if(n == 0) {
                        if(tenet_epoll_ctl(handle, EPOLL_CTL_DEL, fd, NULL) == -1) {
                            printf("Err epoll ctl \n");
                        }
                    }else {
                        printf("receive msg : %s \n", buf);
                    }
                }
            }
        }
    }
    int close = tenet_epoll_close(handle);
    if(close == -1) {
        printf("Error at close epoll : %d \n", tenet_wsa_get_last_error());
        return -1;
    }
    int clean = tenet_wsa_clean_up();
    if(clean == SOCKET_ERROR) {
        printf("Error at cleaning up : %d \n", tenet_wsa_get_last_error());
        return -1;
    }
    return 0;
}

// 创建epoll
void* tenet_epoll_create() {
    // the size parameter means noting now, must be positive
    return epoll_create(1);
}

// epoll_ctl,失败则返回-1，成功则返回0
int tenet_epoll_ctl(void* handle, int op, int socket, struct epoll_event* event) {
    return epoll_ctl(handle, op, socket, event);
}

// epoll_wait,出现错误则返回-1，否则返回触发的事件个数
int tenet_epoll_wait(void* handle, struct epoll_event* events, int maxevents, int timeout) {
    return epoll_wait(handle, events, maxevents, timeout);
}

// epoll_close,出现错误则返回-1，否则返回0
int tenet_epoll_close(void* handle) {
    return epoll_close(handle);
}

// accept 连接,出现错误则返回-1，否则返回建立的fd
int tenet_accept(int socket, struct sockaddr_in* clientAddr, int* clientAddrSize, char* addrStr, int* addrPort) {
    int result = accept(socket, (struct sockaddr *) clientAddr, clientAddrSize);
    if(result == INVALID_SOCKET) {
        return -1;
    }else {
        inet_ntop(AF_INET, clientAddr, addrStr, INET_ADDRSTRLEN);
        *addrPort = ntohs(clientAddr -> sin_port);
        return result;
    }
}

// 创建socket,失败则返回-1，成功则返回socket值
int tenet_socket_create() {
    SOCKET servSock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if(servSock == INVALID_SOCKET) {
        return -1;
    }else {
        return servSock;
    }
}

// 设置socket选项,失败则返回-1，成功则返回0
int tenet_set_socket_opt(int socket, int level, int opt, void* ptr, int size) {
    int result = setsockopt(socket, level, opt, ptr, size);
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 设置IO模式,失败则返回-1，成功则返回0
int tenet_ioctl_socket(int socket, int cmd, u_long* argp) {
    int result = ioctlsocket(socket, cmd, argp);
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 设置sock addr,失败则返回-1，如果address字符串不合法会返回0，否则返回1
int tenet_set_sockaddr(struct sockaddr_in* sockAddr, char* address, int port) {
    memset(sockAddr, 0, sizeof(struct sockaddr_in));  //每个字节都用0填充
    sockAddr -> sin_family = AF_INET;  //使用IPv4地址
    int i = inet_pton(AF_INET, address == NULL ?  INADDR_ANY : address, &(sockAddr -> sin_addr)); //设置IP地址
    sockAddr -> sin_port = htons(port);  //设置端口
    return i;
}

// 绑定socket到固定端口,失败则返回-1，成功则返回0
int tenet_bind(struct sockaddr_in* sockAddr, int socket) {
    int result = bind(socket, (SOCKADDR*) sockAddr, sizeof(SOCKADDR));
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}

// 设置socket监听,失败则返回-1，成功则返回0
int tenet_listen(int socket, int backlog) {
    int result = listen(socket, SOMAXCONN_HINT(128));
    if(result == SOCKET_ERROR) {
        return -1;
    }
    return result;
}


// 获取上一个WSA错误码
int tenet_wsa_get_last_error() {
    return WSAGetLastError();
}

// 获取当前errno
int tenet_errno() {
    return errno;
}

// 清理当前wsa使用
int tenet_wsa_clean_up() {
    return WSACleanup();
}
