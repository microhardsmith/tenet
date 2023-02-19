typedef struct {
    int a;
    int b;
} book;

__declspec(dllexport) int hello(book* ptr);

__declspec(dllexport) int pr(char* str);