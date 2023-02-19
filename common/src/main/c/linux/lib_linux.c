#include <stdio.h>
#include <sys/epoll.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <netinet/tcp.h>
#include <arpa/inet.h> 
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <stdbool.h>
#include <stdlib.h>
#include "lib_linux.h"

static inline int check(int value) {
    if(value == -1) {
        printf("Failed : %d \n", l_errno());
        exit(1);
    }
    return value;
}

int main() {
    int epfd = check(l_epoll_create());
    int addrlen = l_address_len();
    char* addrStr = (char*) malloc(addrlen * sizeof(char));
    memset(addrStr, 0, addrlen);
    struct sockaddr_in serverAddr, clientAddr;
    check(l_set_sock_addr(&serverAddr, addrStr, 10705));
    int socket = check(l_socket_create());
    check(l_set_keep_alive(socket, 0));
    check(l_set_reuse_addr(socket, 1));
    check(l_set_tcp_nodelay(socket, 1));
    check(l_set_nonblocking(socket));
    check(l_bind(&serverAddr, socket, sizeof(struct sockaddr_in)));
    check(l_listen(socket, 10));

    struct epoll_event ev, events[20];
    ev.events = EPOLLIN;
    ev.data.fd = socket;
    check(l_epoll_ctl_add(epfd, socket, &ev));
    while(1) {
        int size = check(l_epoll_wait(epfd, events, 20, -1));
        for(int i = 0; i < size; i++) {
            struct epoll_event event = events[i];
            if(event.data.fd == socket) {
                int client = check(l_accept(socket, &clientAddr, sizeof(clientAddr)));
                check(l_address(&clientAddr, addrStr, l_address_len()));
                int p = l_port(&clientAddr);
                printf("accept from %s:%d \n", addrStr, p);
                l_set_nonblocking(client);
                ev.events = EPOLLIN | EPOLLET;
                ev.data.fd = client;
                check(l_epoll_ctl_add(epfd, client, &ev));
            }else {
                int fd = ev.data.fd;
                if(ev.events & EPOLLIN) {
                    char* buf = (char*)malloc(1024);
                    memset(buf, 0, 1024);
                    int count = 0;
                    int n = 0;
                    while (1) {
                        n = read(fd, (buf + n), 16);
                        printf("step in edge_trigger, read bytes:%d\n", n);
                        if (n > 0) {
                            count += n;
                        } else if (0 == n) {
                            break;
                        } else if (n < 0 && EAGAIN == errno) {
                            printf("errno == EAGAIN, break.\n");
                            break;
                        } else {
                            perror("read failure.");
                            break;
                        }
                    }
                    if (0 == count) {
                        check(l_epoll_ctl_del(epfd, fd));
                        close(fd);
                    }

                    printf("recv from client: %s \n", buf);
                    free(buf);
                }
            }
        }
    }
    check(l_close(socket));
    check(l_close(epfd));
}


// 创建epoll,失败则返回-1,成功则返回生成的fd
int l_epoll_create() {
    // the size parameter of epoll_create() is deprecated
    return epoll_create(1);
}

// 添加epoll事件,失败则返回-1,成功则返回0
int l_epoll_ctl_add(int epfd, int socket, struct epoll_event* ev) {
    return epoll_ctl(epfd, EPOLL_CTL_ADD, socket, ev);
}

// 删除epoll事件,失败则返回-1,成功则返回0
int l_epoll_ctl_del(int epfd, int socket) {
    return epoll_ctl(epfd, EPOLL_CTL_DEL, socket, NULL);
}

// 等待epoll事件,失败则返回-1,成功则返回获取事件的数量，可能为0
int l_epoll_wait(int epfd, struct epoll_event* events, int maxEvents, int timeout) {
    return epoll_wait(epfd, events, maxEvents, timeout);
}

// 返回ip地址字节长度
int l_address_len() {
    return INET_ADDRSTRLEN;
}

// 从sockAddr中获取客户端地址字符串,失败则返回-1,成功则返回0
int l_address(struct sockaddr_in* sockAddr, char* addrStr, int len) {
    const char* ptr = inet_ntop(AF_INET, &(sockAddr -> sin_addr), addrStr, len);
    if(ptr == NULL) {
        return -1;
    }
    return 0;
}

// 获取客户端连接的端口号
int l_port(struct sockaddr_in* sockAddr) {
    return ntohs(sockAddr -> sin_port);
}

// 创建socket，失败则返回-1,成功则返回socket fd
int l_socket_create() {
    return socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
}

// 接受socket连接，失败则返回-1，成功则返回socket fd，在非阻塞情况下，未立刻建立的连接错误码为EAGAIN或EWOULDBLOCK
int l_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize) {
    return accept(socket, (struct sockaddr *) clientAddr, &clientAddrSize);
}

// 设置sock地址，失败则返回-1，成功则返回1，地址为非法字符串则返回0
int l_set_sock_addr(struct sockaddr_in* sockAddr, char* address, int port) {
    memset(sockAddr, 0, sizeof(struct sockaddr_in));
    sockAddr -> sin_family = AF_INET;
    sockAddr -> sin_port = htons(port);
    return inet_pton(AF_INET, address, &(sockAddr -> sin_addr));
}

// 设置reuseAddr选项,失败则返回-1,成功则返回0
int l_set_reuse_addr(int socket, int value) {
    void* ptr = &value;
    return setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, ptr, sizeof(value));
}

// 设置keepalive选项,失败则返回-1,成功则返回0
int l_set_keep_alive(int socket, int value) {
    void* ptr = &value;
    return setsockopt(socket, SOL_SOCKET, SO_KEEPALIVE, ptr, sizeof(value));
}

// 设置tcpnodelay选项,失败则返回-1,成功则返回0
int l_set_tcp_nodelay(int socket, int value) {
    void* ptr = &value;
    return setsockopt(socket, IPPROTO_TCP, TCP_NODELAY, ptr, sizeof(value));
}

// 设置socket为非阻塞,失败则返回-1,成功则返回0
int l_set_nonblocking(int socket) {
    int flag = fcntl(socket, F_GETFD, 0);
    return fcntl(socket, F_SETFL, flag | O_NONBLOCK);
}

// bind端口地址，失败则返回-1，成功则返回0
int l_bind(struct sockaddr_in* sockAddr, int socket, int size) {
    return bind(socket, (struct sockaddr*) sockAddr, size);
}

// listen端口地址，失败则返回-1，成功则返回0
int l_listen(int socket, int backlog) {
    return listen(socket, backlog);
}

// 从socket接受数据，失败则返回-1，成功则返回接受的字节数
int l_recv(int socket, void* buf, int len) {
    return recv(socket, buf, len, 0);
}

// 关闭fd,失败则返回-1，成功则返回0
int l_close(int fd) {
    return close(fd);
}

// 返回当前错误码
int l_errno() {
    return errno;
}
