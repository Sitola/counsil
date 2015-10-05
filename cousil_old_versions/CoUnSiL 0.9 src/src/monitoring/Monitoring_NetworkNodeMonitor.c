#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <statgrab.h>
#include "Monitoring_NetworkNodeMonitor.h"

int sig_winch_flag = 0;


JNIEXPORT jint JNICALL Java_Monitoring_NetworkNodeMonitor_getLocalCPUUsage
    (JNIEnv* env, jobject obj, jint period) {	

        sg_init();

	sg_cpu_percents *perc = NULL;

        if(sg_drop_privileges() != 0){
                fprintf(stderr, "Failed to drop setuid/setgid privileges\n");
                return -1;
        }
	
	/*
	sg_snapshot();
	signal(SIGWINCH, sig_winch_handler);
	*/

	perc = sg_get_cpu_percents();

	sleep(period);
	
	do {
		if (perc == NULL) {
			fprintf(stderr, "perc == NULL\n");
		}
	} while ((perc = sg_get_cpu_percents()) == NULL);

/*	printf("user %2.2f\%\n", perc->user);
	printf("kernel %2.2f\%\n", perc->kernel);
	printf("idle %2.2f\%\n", perc->idle);
	printf("iowait %2.2f\%\n", perc->iowait);
	printf("swap %2.2f\%\n", perc->swap);
	printf("nice %2.2f\%\n", perc->nice);  */

	return 100 - perc->idle;

}
	
JNIEXPORT jint JNICALL Java_Monitoring_NetworkNodeMonitor_getLocalMemoryUsage
  (JNIEnv* env, jobject obj, jint swap) {

  sg_init();
  
  sg_mem_stats *memStats = NULL;
  sg_swap_stats *swapStats = NULL;

  if(sg_drop_privileges() != 0){
    fprintf(stderr, "Failed to drop setuid/setgid privileges\n");
    return -1;
   }
   memStats = sg_get_mem_stats();
   swapStats = sg_get_swap_stats();   

   if(memStats != NULL && swapStats != NULL && memStats->total != 0 && swapStats->total != 0) {
     if(swap) {   
       return (int)(100.00 * (double)(memStats->used + swapStats->used)/(memStats->total + swapStats->total));
     }
     else {
       return (int)(100.00 * (double)(memStats->used)/(memStats->total));
     }
   }
   return -1;
			
  }

JNIEXPORT jobjectArray JNICALL Java_Monitoring_NetworkNodeMonitor_getLocalInterfacesNames
  (JNIEnv *env, jobject obj) {
  sg_init();
  jstring str = NULL;
  int i, p = 1;
  int *numInterfaces = &p;
  sg_network_iface_stats *interfaceStats = NULL;  	 	  
  jclass strCls = (*env)->FindClass(env,"Ljava/lang/String;");
  
  if(sg_drop_privileges() != 0){
     fprintf(stderr, "Failed to drop setuid/setgid privileges\n");
     return;
  }
  
  interfaceStats = sg_get_network_iface_stats(numInterfaces);
 
  jobjectArray strarray = (*env)->NewObjectArray(env,*numInterfaces,strCls,NULL);

  if(interfaceStats != NULL) {
    for(i=0;i < *numInterfaces ; i++, interfaceStats++)
    {
      str = (*env)->NewStringUTF(env,interfaceStats -> interface_name);
      (*env)->SetObjectArrayElement(env,strarray,i,str);
      (*env)->DeleteLocalRef(env,str);
    }
  }
  return strarray;
  
}

JNIEXPORT jint JNICALL Java_Monitoring_NetworkNodeMonitor_getLocalInterfaceUsage
  (JNIEnv *env, jobject obj, jstring ifaceName, jint rxTx, jint period) {
	  
  sg_init();
  int i, p = 1, result = 0;
  int *numInterfaces = &p;
  sg_network_io_stats *netIOStats = NULL;

   if(sg_drop_privileges() != 0){
      fprintf(stderr, "Failed to drop setuid/setgid privileges\n");
      return -1;
   }
   const char *interfaceName = (*env)->GetStringUTFChars(env,ifaceName, NULL);
   
   netIOStats = sg_get_network_io_stats_diff(numInterfaces);
   sleep(period);
   netIOStats = sg_get_network_io_stats_diff(numInterfaces);

   if(netIOStats != NULL){
     for(i=0; i < *numInterfaces; i++, netIOStats++) {
	if (strcmp(netIOStats->interface_name, interfaceName) == 0) {
	  if (rxTx) {	
	    result = netIOStats->tx;
	  }
	  else {
 	    result = netIOStats->rx;
	  }
	  break;
	}
     }
   }
  (*env)-> ReleaseStringUTFChars(env, ifaceName, interfaceName);

  return result;
  }

JNIEXPORT jint JNICALL Java_Monitoring_NetworkNodeMonitor_isLocalInterfaceUp
  (JNIEnv *env, jobject obj, jstring ifaceName) {
  sg_init();
  int i, p = 1, result = 0;
  int *numInterfaces = &p;
  sg_network_iface_stats *interfaceStats = NULL;

  if(sg_drop_privileges() != 0){
     fprintf(stderr, "Failed to drop setuid/setgid privileges\n");
     return -1;
    }
  const char *interfaceName = (*env)->GetStringUTFChars(env,ifaceName, NULL);
  interfaceStats = sg_get_network_iface_stats(numInterfaces);
 
  if(interfaceStats != NULL) { 
    for(i=0; i < *numInterfaces; i++, interfaceStats++) {
      if (strcmp(interfaceStats->interface_name, interfaceName) == 0) {
        result = interfaceStats->up;
        break;
      }
    }
  }

  (*env)-> ReleaseStringUTFChars(env, ifaceName, interfaceName);  
  return result;
 }

