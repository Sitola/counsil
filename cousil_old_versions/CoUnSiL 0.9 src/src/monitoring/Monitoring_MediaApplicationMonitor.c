#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <statgrab.h>
#include <sys/types.h>
#include <ctype.h>
#include <string.h>
#include "Monitoring_MediaApplicationMonitor.h"


JNIEXPORT jlong JNICALL Java_Monitoring_MediaApplicationMonitor_getAllocatedMemory
  (JNIEnv *env, jobject obj, jint pid) {
    sg_process_stats *processStats = NULL;
    int i, p =1;
    int *numProcesses = &p;

    processStats = sg_get_process_stats(numProcesses);
    if (processStats != NULL) {
        for (i=0; i < *numProcesses; i++, processStats++) {
            if (processStats->pid == pid) {
	        return processStats->proc_resident;
	    }
            
        }
    }
    return -1; /*error*/
    
	  
}

JNIEXPORT jdouble JNICALL Java_Monitoring_MediaApplicationMonitor_getCPUUsage
  (JNIEnv *env, jobject obj, jint pid) {
    sg_process_stats *processStats = NULL;
    int i, p =1;
    int *numProcesses = &p;
    processStats = sg_get_process_stats(numProcesses);
    if (processStats != NULL) {
        for (i=0; i < *numProcesses; i++, processStats++) {
            if (processStats->pid == pid) {
                 return processStats->cpu_percent;
            }
        }
   } 
   return -1; /*error*/

}
