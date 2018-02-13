#if defined(__linux__)
    #if defined(__ANDROID__)
        #define OSTYPE_Android /* Android */
    #else
        #define OSTYPE_Linux /* Linux */
    #endif
#elif !defined(_WIN32) && (defined(__unix__) || defined(__unix) || (defined(__APPLE__) && defined(__MACH__)))
    #include <sys/param.h>
    #ifdef __APPLE__   
        #include "TargetConditionals.h"
        #if TARGET_OS_IPHONE 
            #define OSTYPE_iOS /* iOS Device */ */
        #else 
            #define OSTYPE_OSX /* OSX */
        #endif
    #elif defined(BSD)
        #define OSTYPE_BSD /* BSD */
    #endif
#elif defined(_WIN64)
    #define OSTYPE_WIN64 /* Windows x64 */
#elif defined(_WIN32)
    #define OSTYPE_WIN32 /* Windows 32 Bit */
#elif __posix
    #define OSTYPE_POSIX /* At least POSIX compatible */
#elif __unix
    #define OSTYPE_WTF /* some other UNIX not catched above */
#endif
