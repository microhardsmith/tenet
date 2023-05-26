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
    socklen_t addrlen = l_address_len();
    char* addrStr = (char*) malloc(addrlen * sizeof(char));
    memset(addrStr, 0, addrlen);
    struct sockaddr_in serverAddr, clientAddr;
    check(l_set_sock_addr(&serverAddr, addrStr, 10705));
    int socket = check(l_socket_create());
    check(l_set_keep_alive(socket, 0));
    check(l_set_reuse_addr(socket, 1));
    check(l_set_tcp_no_delay(socket, 1));
    check(l_set_nonblocking(socket));
    check(l_bind(socket, &serverAddr, sizeof(struct sockaddr_in)));
    check(l_listen(socket, 10));

    struct epoll_event ev, events[20];
    ev.events = EPOLLIN;
    ev.data.fd = socket;
    check(l_epoll_ctl(epfd, EPOLL_CTL_ADD, socket, &ev));
    while(1) {
        int size = check(l_epoll_wait(epfd, events, 20, -1));
        for(int i = 0; i < size; i++) {
            struct epoll_event event = events[i];
            if(event.data.fd == socket) {
                int client = check(l_accept(socket, &clientAddr, sizeof(clientAddr)));
                check(l_address(&clientAddr, addrStr, addrlen));
                int p = l_port(&clientAddr);
                printf("accept from %s:%d \n", addrStr, p);
                l_set_nonblocking(client);
                ev.events = EPOLLIN;
                ev.data.fd = client;
                check(l_epoll_ctl(epfd, EPOLL_CTL_ADD, client, &ev));
            }else {
                int fd = ev.data.fd;
                if(ev.events & EPOLLIN) {
                    char buf[1024];
                    memset(buf, 0, 1024);
                    int n = l_recv(fd, buf, 1024);
                    if(n < 0) {
                        printf("read fd failure : %d \n", fd);
                    }else if(n == 0) {
                        check(l_epoll_ctl(epfd, EPOLL_CTL_DEL, fd, NULL));
                        l_close(fd);
                    }else {
                        printf("recv from client: %s \n", buf);
                    }
                }
            }
        }
    }
    check(l_close(socket));
    check(l_close(epfd));
}

// 获取标准输入流
FILE* g_stdout() {
    return stdout;
}

// 获取标准错误流
FILE* g_stderr() {
    return stderr;
}

// 向标准输出流输出字符并刷新缓冲区
void g_print(char* str, FILE* stream) {
    fputs(str, stream);
    fflush(stdout);
}

// 返回connect导致阻塞的错误码,在Linux系统下为EINPROGRESS
int l_connect_block_code() {
    return EINPROGRESS;
}

// 返回send导致阻塞的错误码,在Linux系统下为EAGAIN或EWOULDBLOCK
int l_send_block_code() {
    return EAGAIN;
}

// 创建epoll,失败则返回-1,成功则返回生成的fd
int l_epoll_create() {
    // the size parameter of epoll_create() is deprecated
    return epoll_create(1);
}

// 操作epoll事件,失败则返回-1,成功则返回0
int l_epoll_ctl(int epfd, int op, int socket, struct epoll_event* ev) {
    return epoll_ctl(epfd, op, socket, ev);
}

// 等待epoll事件,失败则返回-1,成功则返回获取事件的数量,可能为0
int l_epoll_wait(int epfd, struct epoll_event* events, int maxEvents, int timeout) {
    return epoll_wait(epfd, events, maxEvents, timeout);
}

// 返回ip地址字节长度
socklen_t l_address_len() {
    return INET_ADDRSTRLEN;
}

// 从sockAddr中获取客户端地址字符串,失败则返回-1,成功则返回0
int l_address(struct sockaddr_in* sockAddr, char* addrStr, socklen_t len) {
    const char* ptr = inet_ntop(AF_INET, &(sockAddr -> sin_addr), addrStr, len);
    if(ptr == NULL) {
        return -1;
    }
    return 0;
}

// 获取客户端连接的端口号
uint16_t l_port(struct sockaddr_in* sockAddr) {
    return ntohs(sockAddr -> sin_port);
}

// 创建socket,失败则返回-1,成功则返回socket fd
int l_socket_create() {
    return socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
}

// 接受socket连接,失败则返回-1,成功则返回socket fd,在非阻塞情况下,未立刻建立的连接错误码为EAGAIN或EWOULDBLOCK
int l_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize) {
    return accept(socket, (struct sockaddr *) clientAddr, &clientAddrSize);
}

// 设置sock地址,失败则返回-1,成功则返回1,地址为非法字符串则返回0
int l_set_sock_addr(struct sockaddr_in* sockAddr, char* address, uint16_t port) {
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
int l_set_tcp_no_delay(int socket, int value) {
    void* ptr = &value;
    return setsockopt(socket, IPPROTO_TCP, TCP_NODELAY, ptr, sizeof(value));
}

// 获取指定socket上的错误码,如果socket上无错误应返回0
int l_get_err_opt(int socket, int* ptr) {
    socklen_t ptr_size = sizeof(int);
    return getsockopt(socket, SOL_SOCKET, SO_ERROR, (void*) ptr, &ptr_size);
}

// 设置socket为非阻塞,失败则返回-1,成功则返回0
int l_set_nonblocking(int socket) {
    int flag = fcntl(socket, F_GETFD, 0);
    return fcntl(socket, F_SETFL, flag | O_NONBLOCK);
}

// bind端口地址,失败则返回-1,成功则返回0
int l_bind(int socket, struct sockaddr_in* sockAddr, socklen_t size) {
    return bind(socket, (struct sockaddr*) sockAddr, size);
}

// 客户端建立连接,失败则返回-1,成功则返回0
int l_connect(int socket, struct sockaddr_in* sockAddr, socklen_t size) {
    return connect(socket, (struct sockaddr*) sockAddr, size);
}

// listen端口地址,失败则返回-1,成功则返回0
int l_listen(int socket, int backlog) {
    return listen(socket, backlog);
}

// 从socket接受数据,失败则返回-1,成功则返回接受的字节数
ssize_t l_recv(int socket, void* buf, size_t len) {
    return recv(socket, buf, len, 0);
}

// 向socket发送数据,失败则返回-1,成功则返回已接收字节数
ssize_t l_send(int socket, void* buf, size_t len) {
    return send(socket, buf, len, 0);
}

// 关闭fd,失败则返回-1,成功则返回0
int l_close(int fd) {
    return close(fd);
}

// 关闭fd写端,失败则返回-1,成功则返回0
int l_shutdown_write(int fd) {
    return shutdown(fd, SHUT_WR);
}

// 返回当前错误码
int l_errno() {
    return errno;
}
