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
    check(m_bind(socket, &serverAddr, sizeof(struct sockaddr_in)));
    check(m_listen(socket, 10));

    check(m_kevent_ctl(kq, socket, EVFILT_READ, EV_ADD));
    struct kevent eventlist[32];
    while(1) {
        int size = m_kevent_wait(kq, eventlist, 32, NULL);
        for(int i = 0; i < size; i++) {
            struct kevent event = eventlist[i];
            int fd = event.ident;
            if(fd == socket) {
                int client = check(m_accept(socket, &clientAddr, sizeof(clientAddr)));
                check(m_address(&clientAddr, addrStr, addrlen));
                int p = m_port(&clientAddr);
                printf("accept from %s:%d \n", addrStr, p);
                check(m_set_nonblocking(client));
                check(m_kevent_ctl(kq, client, EVFILT_READ, EV_ADD));
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

// 获取标准输入流
FILE* g_stdout() {
    return stdout;
}

// 获取标准错误流
FILE* g_stderr() {
    return stderr;
}

// 返回connect导致阻塞的错误码,在macos下为EINPROGRESS
int m_connect_block_code() {
    return EINPROGRESS;
}

// 返回send导致阻塞的错误码,在macos下为EAGAIN
int m_send_block_code() {
    return EAGAIN;
}

// 创建kqueue,失败则返回-1,成功则返回fd
int m_kqueue() {
    return kqueue();
}

// 向kqueue中注册事件,失败则返回-1,成功则返回0 changelist应只包含一个元素,如果changelist的flag为EV_ERROR则表示出现了错误
int m_kevent_ctl(int kq, int fd, int16_t filter, uint16_t flags) {
    struct kevent event;
    EV_SET(&event, fd, filter, flags, 0, 0, NULL);
    return kevent(kq, &event, 1, NULL, 0, NULL);
}

// 阻塞等待事件,失败则返回-1,成功则返回事件个数
int m_kevent_wait(int kq, struct kevent* eventlist, int nevents, struct timespec* timeout) {
    return kevent(kq, NULL, 0, eventlist, nevents, timeout);
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
uint16_t m_port(struct sockaddr_in* sockAddr) {
    return ntohs(sockAddr -> sin_port);
}

// 创建socket,失败则返回-1,成功则返回fd
int m_socket_create() {
    return socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
}

// 接受socket连接,失败则返回-1,成功则返回socket fd,在非阻塞情况下,未立刻建立的连接错误码为EAGAIN或EWOULDBLOCK
int m_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize) {
    return accept(socket, (struct sockaddr *) clientAddr, &clientAddrSize);
}

// 设置sock地址,失败则返回-1,成功则返回1,地址为非法字符串则返回0
int m_set_sock_addr(struct sockaddr_in* sockAddr, char* address, uint16_t port) {
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

// 获取指定socket上的错误码,如果socket上无错误应返回0
int m_get_err_opt(int socket, int* ptr) {
    socklen_t ptr_size = sizeof(int);
    return getsockopt(socket, SOL_SOCKET, SO_ERROR, (void*) ptr, &ptr_size);
}

// 设置socket为非阻塞,失败则返回-1,成功则返回0
int m_set_nonblocking(int socket) {
    int flag = fcntl(socket, F_GETFD, 0);
    return fcntl(socket, F_SETFL, flag | O_NONBLOCK);
}

// bind端口地址,失败则返回-1,成功则返回0
int m_bind(int socket, struct sockaddr_in* sockAddr, socklen_t size) {
    return bind(socket, (struct sockaddr*) sockAddr, size);
}

// 连接指定地址,失败则返回-1,成功则返回0
int m_connect(int socket, struct sockaddr_in* sockAddr, socklen_t size) {
    return connect(socket, (struct sockaddr*) sockAddr, size);
}

// listen端口地址,失败则返回-1,成功则返回0
int m_listen(int socket, int backlog) {
    return listen(socket, backlog);
}

// 返回EINPROGRESS的值,用于判定非阻塞socket是否在连接中
int m_err_inprogress() {
    return EINPROGRESS;
}

ssize_t m_send(int socket, void* buf, size_t len) {
    return send(socket, buf, len, 0);
}

// 从socket接受数据,失败则返回-1,成功则返回接受的字节数
ssize_t m_recv(int socket, void* buf, size_t len) {
    return recv(socket, buf, len, 0);
}

// 关闭fd,失败则返回-1,成功则返回0
int m_close(int fd) {
    return close(fd);
}

// 关闭socket写端,失败则返回-1,成功则返回0
int m_shutdown_write(int fd) {
    return shutdown(fd, SHUT_WR);
}

// 返回当前错误码
int m_errno() {
    return errno;
}

