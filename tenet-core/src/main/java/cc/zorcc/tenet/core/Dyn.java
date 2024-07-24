package cc.zorcc.tenet.core;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 *   Dyn is short for Dynamic library loading stuff, this is a helper class
 */
public final class Dyn {
    /**
     *   Dyn shouldn't be initialized
     */
    private Dyn() {
        throw new UnsupportedOperationException();
    }

    /**
     *   System native linker
     */
    private static final Linker linker = Linker.nativeLinker();

    /**
     *   Default dynamic library loading path
     */
    private static final String libPath = Std.normalizePath(findLibPath());

    /**
     *   Ext format
     */
    private static final String extFormat = extFormat();

    /**
     *   Sys lib path
     */
    private static final String sysPath = Std.normalizePath(sysPath());

    /**
     *   Tenet search dynamic library loading path in 3 steps:
     *   1. search OS env
     *   2. search property arguments, could be passed from -DTENET_LIBRARY_PATH=...
     *   3. fallback to the same directory of the program
     */
    private static String findLibPath() {
        // checkout env
        String envLibPath = System.getenv(Constants.TENET_LIBRARY_PATH);
        if(envLibPath != null && !envLibPath.isBlank()) {
            return envLibPath;
        }
        // checkout properties
        String propertyLibPath = System.getProperty(Constants.TENET_LIBRARY_PATH);
        if(propertyLibPath != null && !propertyLibPath.isBlank()) {
            return propertyLibPath;
        }
        // fallback to user.dir
        String userDir = System.getProperty("user.dir");
        if(userDir != null && !userDir.isBlank()) {
            return userDir;
        }
        throw new ExceptionInInitializerError();
    }

    /**
     *   Finding methodHandle from VM implementation
     */
    public static MethodHandle vmMh(String methodName, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        if(methodName == null || methodName.isBlank()) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED);
        }
        MemorySegment funcPtr = linker.defaultLookup().find(methodName).orElseThrow(() -> new TenetException(ExceptionType.NATIVE, "Method not found : %s".formatted(methodName)));
        return linker.downcallHandle(funcPtr, functionDescriptor, options);
    }

    /**
     *   Finding target native method from current SymbolLookup, throw an exception if not found
     */
    public static MethodHandle mh(SymbolLookup lookup, String methodName, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        return mh(lookup, List.of(methodName), functionDescriptor, options);
    }

    /**
     *   Finding target native methods from current SymbolLookup, throw an exception if not found
     *   Due to C macros, different OS and different distros, implementation could differ in names, this function would traverse each methodName to see if there is a match
     */
    public static MethodHandle mh(SymbolLookup lookup, List<String> methodNames, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        for (String methodName : methodNames) {
            Optional<MemorySegment> ptr = lookup.find(methodName);
            if(ptr.isPresent()) {
                return linker.downcallHandle(ptr.get(), functionDescriptor, options);
            }
        }
        throw new TenetException(ExceptionType.NATIVE, "Target method not found in : %s".formatted(methodNames));
    }

    /**
     *   Load dynamic library, return the SymbolLookup associated with it
     *   Note that, for a process, a dynamic library could be loaded many times, but only one copy would exist in process's memory
     *   So, calling loadDynLibrary() with same libraryName multiple times will always return the SymbolLookup targeting at the same area of the process memory with no harm
     */
    public static SymbolLookup loadDynLibrary(String libraryName) {
        if(libraryName == null || libraryName.isBlank()) {
            throw new TenetException(ExceptionType.NATIVE, "Library name is empty");
        }
        return SymbolLookup.libraryLookup(concatDynLibraryPath(libraryName), Arena.global());
    }

    /**
     *   Concat absolute library path with naming convention
     */
    private static String concatDynLibraryPath(String libraryName) {
        if(extFormat == null) {
            throw new TenetException(ExceptionType.NATIVE, "Extension format not found");
        }
        String targetName = libraryName + extFormat;
        // Try system library if provided by operating system
        if(sysPath != null) {
            File f = new File(sysPath);
            if(f.isDirectory()) {
                for (String fileName : Objects.requireNonNull(f.list())) {
                    // Due to operating system's version control, system dynamic library could have some weird suffix, so we use startsWith() here to filter them
                    if(fileName.startsWith(targetName) && !Files.isSymbolicLink(Path.of(fileName))) {
                        return sysPath + "/" + fileName;
                    }
                }
            }else {
                throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED);
            }
        }
        // Try library path from env
        String path = libPath + "/" + targetName;
        if(Files.exists(Path.of(path))) {
            return path;
        }
        // Try some alternative approach by providing some naming convention between Windows users and Linux users
        String alternativePath = libraryName.startsWith("lib") ? libPath + "/" + libraryName.substring("lib".length()) + extFormat : libPath + "/lib" + targetName;
        if(Files.exists(Path.of(alternativePath))) {
            return alternativePath;
        }
        throw new TenetException(ExceptionType.NATIVE, "Dynamic library not found : %s".formatted(libraryName));
    }

    /**
     *  Get the ext format on target operating system, return null if not found
     */
    private static String extFormat() {
        Os os = Std.os();
        return switch (os) {
            case Windows -> ".dll";
            case Linux -> ".so";
            case macOS -> ".dylib";
            case null, default -> null;
        };
    }

    /**
     *  Get the sys lib path on target operating system
     */
    private static String sysPath() {
        Os os = Std.os();
        return switch (os) {
            case Linux -> "/usr/lib64";
            case macOS -> "/usr/local/lib";
            case null, default -> null;
        };
    }
}
