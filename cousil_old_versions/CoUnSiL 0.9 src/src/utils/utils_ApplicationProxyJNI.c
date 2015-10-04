#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <statgrab.h>
#include <signal.h>
#include <fcntl.h>
#include <limits.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <ctype.h>
#include <string.h>
#include <errno.h>
#include "Monitoring_MediaApplicationMonitor.h"

#define BUFFER_SIZE PIPE_BUF

JNIEXPORT jintArray JNICALL Java_utils_ApplicationProxyJNI_runApplication
  (JNIEnv *env, jobject obj, jstring path, jstring cmdOpt) {

    int ret, res;
    int i = 0;
    int word = 0; /*boolean whether it is inside word or white space*/
    int numberOfWords = 0;
    char **cmdOptField = NULL;
    pid_t pid;
    const char *applicationPath = (*env)->GetStringUTFChars(env, path, NULL);
    const char *cmdOptStep = (*env)->GetStringUTFChars(env, cmdOpt, NULL);
    char *cmdOptCopy = NULL;
    int outpipe[2];
    jintArray result = (*env)->NewIntArray(env,3);

    int * pid2;
    int * parentRead;

    #define PARENT_IN outpipe[0]
    #define CHILD_OUT outpipe[1]
    
  /*  if (cmdOptStep != NULL) {
        cmdOptCopy = (char*)malloc(strlen(cmdOptStep)*sizeof(char));
        strcpy(cmdOptCopy,cmdOptStep);
    } */

    /* application arguments preparation 
    while(cmdOptCopy[i] != '\0') {
        if(!(isspace(cmdOptCopy[i]) || word)) { / *new word
            word = 1;
            numberOfWords++;
            cmdOptField = (char**)realloc(cmdOptField,numberOfWords*sizeof(char*));
            cmdOptField[numberOfWords-1] = cmdOptCopy + i;
        }

        if(isspace(cmdOptCopy[i]) && word) { / *end of word
            word = 0;
	    cmdOptCopy[i] = 0;
	}
	i++;
    } */

    
   if(pipe(outpipe) < 0) {
	   perror("Can't create out pipe");
   }
    
    switch(pid = fork()) {
           case -1:
                   perror("fork(): ");
		   (*env)-> SetIntArrayRegion(env, result, 0, 1, (jint *)(-1));
                   return result;
           case 0:
		   close(1);
		   dup(CHILD_OUT); 
		   close(CHILD_OUT);
		   close(PARENT_IN);
		 //  
		  /* if(access(stdoutFIFO, F_OK) == -1) {
			   res = mkfifo(stdoutFIFO, 0777);
			   if (res != 0) {
				   perror("can't create fifo");
			   }
		   }
		   res = open(stdoutFIFO, O_WRONLY | O_NONBLOCK);
		   freopen(stdoutFIFO, "a", stdout);*/

                   ret = execl(applicationPath, applicationPath, cmdOptStep,0);
                   if (ret == -1) {
                           perror("execl(): ");
    			   (*env)-> SetIntArrayRegion(env, result, 0, 1, (jint *)(-1));
                           return result;
                   }
		   break;
          default:
		   close(CHILD_OUT);
                   free(cmdOptCopy);
                   free(cmdOptField);
                   (*env)-> ReleaseStringUTFChars(env, path, applicationPath);
                   (*env)-> ReleaseStringUTFChars(env, cmdOpt, cmdOptStep);
 		   pid2 = &pid;		   
		   parentRead= &PARENT_IN;
		   (*env)-> SetIntArrayRegion(env, result, 0, 1, pid2); 
		   (*env)-> SetIntArrayRegion(env, result, 1, 1, parentRead); 
		   //(*env)-> SetIntArrayRegion(env, result, 2, 1, 2);
                    return result;
    }
}

JNIEXPORT jint JNICALL Java_utils_ApplicationProxyJNI_isNotRunning
  (JNIEnv *env, jobject obj, jint pid){
    sg_process_stats *processStats = NULL;
    int status, result, err;

    result = waitpid(pid, &status, WNOHANG);
    err = errno;

    if(result == -1) {
	    if(err == ECHILD) {
		    return 1;
	    }
	    perror("waitpid(): ");
	    return -1;
    }

    if(result == 0) { /*process didn't changed state (it's running) */
	    return result;
    }
    if(result == pid) { /*process changed state*/
	    if (WIFEXITED(status)) { /*terminated normally*/
		    return 1;
	    }
	    if(WIFSIGNALED(status)) {
		    return WTERMSIG(status);
	    }
	    return 0;
    }
    return -1;
}
JNIEXPORT jint JNICALL Java_utils_ApplicationProxyJNI_interruptApplication
  (JNIEnv *env, jobject obj, jint pid) {
	  int status;
	  int killReturn = kill(pid, SIGINT);
	  if(killReturn == -1) {
	        perror("kill(): ");
	        return -1;
	  }
	  return killReturn;
	  
	  
  }

JNIEXPORT jint JNICALL Java_utils_ApplicationProxyJNI_terminateApplication
  (JNIEnv *env, jobject obj, jint pid) {
	  int status;
	  int killReturn = kill(pid, SIGTERM);
          if(killReturn == -1) {
                  perror("kill(): ");
                  return -1;
           }
          return killReturn;
  }

JNIEXPORT jint JNICALL Java_utils_ApplicationProxyJNI_killApplication
  (JNIEnv *env, jobject obj, jint pid) {	  
  int status;
  int killReturn = kill(pid, SIGKILL);
  if(killReturn == -1) {
       perror("kill(): ");
       return -1;
  }
  return waitpid(pid, &status, 0);
  
}


JNIEXPORT jstring JNICALL Java_utils_ApplicationProxyJNI_readStdOutPipe
  (JNIEnv *env, jobject obj, jint pipefd) {
  int pipe_fd, res;
  char buffer[BUFFER_SIZE + 1];
  
  memset(buffer, '\0', sizeof(buffer));
  pipe_fd = pipefd;
   
   if(pipe_fd != -1)  {
	    res = read(pipe_fd, buffer, BUFFER_SIZE);
            printf(buffer);
   } 

  return  (*env)->NewStringUTF(env, buffer);

}

