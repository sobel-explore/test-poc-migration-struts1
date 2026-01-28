package util;

public class MemoryUtil {
	
	private static final int MegaBytes = 10241024;
	private final long freeMemory; // approximation to the total amount of memory currently available for future allocated objects
	private final long totalMemory; //  the total amount of memory in the Java virtual machine. The value returned by this method may vary over time, depending on the host environment
	private final long maxMemory; // the maximum amount of memory that the Java virtual machine will attempt to use
	private final long memoryUsed; // Used memory in JVM
	
	public MemoryUtil(){
        freeMemory = Runtime.getRuntime().freeMemory()/MegaBytes;
        totalMemory = Runtime.getRuntime().totalMemory()/MegaBytes;
        maxMemory = Runtime.getRuntime().maxMemory()/MegaBytes;
        memoryUsed = maxMemory - freeMemory; 
//        System.out.println("Used Memory in JVM: " + memoryUsed);
//        System.out.println("freeMemory in JVM: " + freeMemory);
//        System.out.println("totalMemory in JVM shows current size of java heap: " + totalMemory);
//        System.out.println("maxMemory in JVM: " + maxMemory);
	}

	public final long getFreeMemory() {
		return freeMemory;
	}

	public final long getTotalMemory() {
		return totalMemory;
	}

	public final long getMaxMemory() {
		return maxMemory;
	}

	public final long getMemoryUsed() {
		return memoryUsed;
	}
	
	
	

}


