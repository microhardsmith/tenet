#include <stdio.h>
#include <sys/socket.h>
#include <sys/event.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include "lib_macos.h"

static inline int check(int value) {
    if(value == -1) {
        printf("Failed : %d \n", m_errno());
        exit(1);
    }
    return value;
}

int main() {
    int kq = check(m_kqueue());
    socklen_t addrlen = m_address_len();
    char* addrStr = (char*) malloc(addrlen * sizeof(char));
    memset(addrStr, 0, addrlen);
    struct sockaddr_in serverAddr, clientAddr;
    check(m_set_sock_addr(&serverAddr, addrStr, 10705));
    int socket = m_socket_create();
    check(m_set_keep_alive(socket, 0));
    check(m_set_reuse_addr(socket, 1));
    check(m_set_tcp_no_delay(socket, 1));
    check(m_set_nonblocking(socket));
    check(m_bind(&serverAddr, socket, sizeof(struct sockaddr_in)));
    check(m_listen(socket, 10));

    struct kevent changelist;
    check(m_kevent_add(kq, &changelist, socket, EVFILT_READ));
    struct kevent eventlist[32];
    while(1) {
        int size = m_kevent_wait(kq, eventlist, 32);
        for(int i = 0; i < size; i++) {
            struct kevent event = eventlist[i];
            int fd = event.ident;
            if(fd == socket) {
                int client = check(m_accept(socket, &clientAddr, sizeof(clientAddr)));
                check(m_address(&clientAddr, addrStr, addrlen));
                int p = m_port(&clientAddr);
                printf("accept from %s:%d \n", addrStr, p);
                check(m_set_nonblocking(client));
                check(m_kevent_add(kq, &changelist, client, EVFILT_READ));
            }else if(event.flags & EV_EOF) {
                printf("client %d disconnected \n", fd);
                // When the client disconnects an EOF is sent. By closing the file
                // descriptor the event is automatically removed from the kqueue.
                check(m_close(fd));
            }else if(event.filter == EVFILT_READ) {
                char buf[1024];
                size_t count = m_recv(fd, buf, sizeof(buf));
                printf("receive msg : %s, current %lu bytes \n", buf, count);
            }
        }
    }
}

// 向标准输出流输出字符并刷新缓冲区
void g_print(char* str) {
    puts(str);
    fflush(stdout);
}

// 创建kqueue,失败则返回-1，成功则返回fd
int m_kqueue() {
    return kqueue();
}

// 向kqueue中注册事件,失败则返回-1，成功则返回0 changelist应只包含一个元素
int m_kevent_add(int kq, struct kevent* changelist, int fd, uint16_t flag) {
    memset(changelist, 0, sizeof(struct kevent));
    EV_SET(changelist, fd, flag, EV_ADD, 0, 0, NULL);
    return kevent(kq, changelist, 1, NULL, 0, NULL);
}

// 阻塞等待事件,失败则返回-1，成功则返回事件个数
int m_kevent_wait(int kq, struct kevent* eventlist, int nevents) {
    return kevent(kq, NULL, 0, eventlist, nevents, NULL);
}

// 返回ip地址字节长度
socklen_t m_address_len() {
    return INET_ADDRSTRLEN;
}

// 从sockAddr中获取客户端地址字符串,失败则返回-1,成功则返回0
int m_address(struct sockaddr_in* sockAddr, char* addrStr, socklen_t len) {
    const char* ptr = inet_ntop(AF_INET, &(sockAddr -> sin_addr), addrStr, len);
    if(ptr == NULL) {
        return -1;
    }
    return 0;
}

// 获取客户端连接的端口号
int m_port(struct sockaddr_in* sockAddr) {
    return ntohs(sockAddr -> sin_port);
}

// 创建socket，失败则返回-1，成功则返回fd
int m_socket_create() {
    return socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
}

// 接受socket连接，失败则返回-1，成功则返回socket fd，在非阻塞情况下，未立刻建立的连接错误码为EAGAIN或EWOULDBLOCK
int m_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize) {
    return accept(socket, (struct sockaddr *) clientAddr, &clientAddrSize);
}

// 设置sock地址，失败则返回-1，成功则返回1，地址为非法字符串则返回0
int m_set_sock_addr(struct sockaddr_in* sockAddr, char* address, int port) {
    memset(sockAddr, 0, sizeof(struct sockaddr_in));
    sockAddr -> sin_family = AF_INET;
    sockAddr -> sin_port = htons(port);
    return inet_pton(AF_INET, address, &(sockAddr -> sin_addr));
}

// 设置reuseAddr选项,失败则返回-1,成功则返回0
int m_set_reuse_addr(int socket, int value) {
    void* ptr = &value;
    return setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, ptr, sizeof(value));
}

// 设置keepalive选项,失败则返回-1,成功则返回0
int m_set_keep_alive(int socket, int value) {
    void* ptr = &value;
    return setsockopt(socket, SOL_SOCKET, SO_KEEPALIVE, ptr, sizeof(value));
}

// 设置tcpnodelay选项,失败则返回-1,成功则返回0
int m_set_tcp_no_delay(int socket, int value) {
    void* ptr = &value;
    return setsockopt(socket, IPPROTO_TCP, TCP_NODELAY, ptr, sizeof(value));
}

// 设置socket为非阻塞,失败则返回-1,成功则返回0
int m_set_nonblocking(int socket) {
    int flag = fcntl(socket, F_GETFD, 0);
    return fcntl(socket, F_SETFL, flag | O_NONBLOCK);
}

// bind端口地址，失败则返回-1，成功则返回0
int m_bind(struct sockaddr_in* sockAddr, int socket, socklen_t size) {
    return bind(socket, (struct sockaddr*) sockAddr, size);
}

// listen端口地址，失败则返回-1，成功则返回0
int m_listen(int socket, int backlog) {
    return listen(socket, backlog);
}

// 从socket接受数据，失败则返回-1，成功则返回接受的字节数
ssize_t m_recv(int socket, void* buf, socklen_t len) {
    return recv(socket, buf, len, 0);
}

// 关闭fd,失败则返回-1，成功则返回0
int m_close(int fd) {
    return close(fd);
}

// 返回当前错误码
int m_errno() {
    return errno;
}

