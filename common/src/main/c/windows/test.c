#include <stdio.h>
#include "test.h"
// #include "wepoll.h"
// #include <WinSock2.h>

// #pragma comment (lib, "ws2_32.lib")

int hello(book* ptr) {
    printf("c address : %p \n", ptr);
    ptr->a = 4;
    ptr->b = 5;
    return 0;
}

int pr(char* str) {
    return puts(str);
}

int main() {
    char* str = "hello";
    for(int i = 0; i < 1000; i++) {
        pr(str);
    }
}