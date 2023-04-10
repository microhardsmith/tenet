#ifndef LIB_LINUX_H
#define LIB_LINUX_H

void g_print(char* str);

int l_connect_block_code();

int l_send_block_code();

int l_epoll_create();

int l_epoll_ctl_add(int epfd, int socket, struct epoll_event* ev);

int l_epoll_ctl_del(int epfd, int socket);

int l_epoll_wait(int epfd, struct epoll_event* events, int maxEvents, int timeout);

socklen_t l_address_len();

int l_address(struct sockaddr_in* sockAddr, char* addrStr, socklen_t len);

int l_port(struct sockaddr_in* sockAddr);

int l_socket_create();

int l_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize);

int l_set_sock_addr(struct sockaddr_in* sockAddr, char* address, int port);

int l_set_reuse_addr(int socket, int value);

int l_set_keep_alive(int socket, int value);

int l_set_tcp_no_delay(int socket, int value);

int l_get_err_opt(int socket);

int l_set_nonblocking(int socket);

int l_bind(int socket, struct sockaddr_in* sockAddr, socklen_t size);

int l_connect(int socket, struct sockaddr_in* sockAddr, socklen_t size);

int l_listen(int socket, int backlog);

ssize_t l_recv(int socket, void* buf, size_t len);

ssize_t l_send(int socket, void* buf, size_t len);

int l_close(int fd);

int l_errno();

#endif