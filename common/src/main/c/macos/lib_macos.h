#ifndef LIB_MACOS_H
#define LIB_MACOS_H

void g_puts(char* str);

void g_flush();

int m_kqueue();

int m_kevent_add(int kq, struct kevent* changelist, int fd, uint16_t flag);

int m_kevent_wait(int kq, struct kevent* eventlist, int nevents);

socklen_t m_address_len();

int m_address(struct sockaddr_in* sockAddr, char* addrStr, socklen_t len);

int m_port(struct sockaddr_in* sockAddr);

int m_socket_create();

int m_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize);

int m_set_sock_addr(struct sockaddr_in* sockAddr, char* address, int port);

int m_set_reuse_addr(int socket, int value);

int m_set_keep_alive(int socket, int value);

int m_set_tcp_no_delay(int socket, int value);

int m_set_nonblocking(int socket);

int m_bind(struct sockaddr_in* sockAddr, int socket, socklen_t size);

int m_listen(int socket, int backlog);

ssize_t m_recv(int socket, void* buf, socklen_t len);

int m_close(int fd);

int m_errno();

#endif