#ifndef TENET_LIB_WIN_H
#define TENET_LIB_WIN_H

__declspec(dllexport) void* tenet_epoll_create();

__declspec(dllexport) int tenet_epoll_ctl(void* handle, int op, int socket, struct epoll_event* event);

__declspec(dllexport) int tenet_epoll_wait(void* handle, struct epoll_event* events, int maxevents, int timeout);

__declspec(dllexport) int tenet_epoll_close(void* handle);

__declspec(dllexport) int tenet_accept(int socket, struct sockaddr_in* clientAddr, int* clientAddrSize, char* addrStr, int* addrPort);

__declspec(dllexport) int tenet_socket_create();

__declspec(dllexport) int tenet_set_socket_opt(int socket, int level, int opt, void* ptr, int size);

__declspec(dllexport) int tenet_ioctl_socket(int socket, int cmd, u_long* argp);

__declspec(dllexport) int tenet_set_sockaddr(struct sockaddr_in* sockAddr, char* address, int port);

__declspec(dllexport) int tenet_bind(struct sockaddr_in* sockAddr, int socket);

__declspec(dllexport) int tenet_listen(int socket, int backlog);

__declspec(dllexport) int tenet_wsa_get_last_error();

__declspec(dllexport) int tenet_errno();

__declspec(dllexport) int tenet_wsa_clean_up();

#endif