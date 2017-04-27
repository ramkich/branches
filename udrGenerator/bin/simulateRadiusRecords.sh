#!/bin/sh
#===================================================================
# Create Simulated data volumes by market
#
# Rev: 2016-02-04 - Ramakrishna Chekuri
#      Introduced as part of the Aliant UBB Phase 2 Project
#===================================================================


#==============================================
# NM_ROOT_DIR = /app/cgi, unless already defined
#==============================================

NM_ROOT_DIR="/app/cgi"
NM_APPNAME=radius
NM_USER=radius


if [ x$LOGNAME != x"${NM_USER}" ]
then
   NM_ROOT_DIR="/develop/workspace/$LOGNAME"
fi

NM_APP_DIR="${NM_ROOT_DIR}/networkMediation/applications/${NM_APPNAME}"
NM_APP_CURRENT_DIR=${NM_APP_DIR}/current

LOG_FILE=${NM_APP_CURRENT_DIR}/log/`basename $0 .sh`.log

OS_=`uname`
CAL_='/bin/cal'
PS_='/usr/bin/ps'
SENDMAIL_='/usr/lib/sendmail'
if [ "$OS_" = "Linux" ] ; then
   CAL_='/usr/bin/cal'
   PS_='/bin/ps'
   SENDMAIL_='/usr/sbin/sendmail'
fi

NOW=`date +%Y%m%d-%H:%M:%S`

echo " "
echo "==================================================================="
echo "Script $(basename ${0}) ${@} started on `date`"
echo "==================================================================="
echo " "

echo " "  >> ${LOG_FILE}
echo "==================================================================="    >> ${LOG_FILE}
echo "Script $(basename ${0}) ${@} started on `date`"   >> ${LOG_FILE}



###############################################################################
#
#   Log start time time
#
echo "$(basename ${0}) ${@} script started on `date`" >> $LOG_FILE

#===================================================================
# Usage display
#===================================================================
usage() {
    echo "Usage: $0 [-t hse-sympatico=800000,hse-business=400000,hse-virgin=30000,hse-atlantic=650000,hse-wholesale=20000] [-s 2016020417]"
    exit
}


#===================================================================
# Read global configuration file
#===================================================================

CONFIG_FILENAME="radius.conf"
CONFIG_FILE=${NM_APP_CURRENT_DIR}/shared/bin/${CONFIG_FILENAME}

if [ -f $CONFIG_FILE ]
then
   . $CONFIG_FILE
else
   echo "*** Global configuration file not found!"
   echo "    $CONFIG_FILE : no such file"
   exit
fi


#===================================================================
#
# read command line.
#
#===================================================================

while getopts t:s: c
do
    case $c in
        t)  ACCT_TOS="$OPTARG"
            ;;
        s)  RECORD_TIMESTAMP="$OPTARG"
            ;;       
        \?) usage
            exit 1
            ;;
    esac
done
shift `expr $OPTIND - 1`


if [ -z "${ACCT_TOS}" ] || [ "x$ACCT_TOS" = "x" ] || [ -z "${RECORD_TIMESTAMP}" ] || [ "x$RECORD_TIMESTAMP" = "x" ]
then
     usage
     exit 1
fi


PARAM_ACCT_TOS="-acctTos $ACCT_TOS"
PARAM_RECORD_TIMESTAMP="-recordTimestamp $RECORD_TIMESTAMP"
CLASS_NAME="SimulatedDataManagerMain"
APP_LOG="${NM_LOG_DIR}/simulateRadiusRecords.log"


#==============================================
# Verify that a process is not already running
#==============================================

NM_FLAG_FILE_DIR="${NM_FILESTORAGE_DIR}/flagFiles"

#Create the directory for the flagfile
if [ ! -d "${NM_FLAG_FILE_DIR}" ]
then
   `mkdir ${NM_FLAG_FILE_DIR}`
fi

FILENAME="pid_radiusReport_$REPORT_TYPE"

FLAG_FILE="$NM_FLAG_FILE_DIR/$FILENAME.txt"


if [ -f "${FLAG_FILE}" ]
   then

   # Check if a process is running

   PID=`cat ${FLAG_FILE}`
   echo "pid $PID"
   OUTPUT=`$PS_ -p "${PID}"`

   if [ $? = 0 ]
   then

      # Notify the team that the a process is still running
      echo "ERROR - `date` Another instance (PID = $PID) is processing. Abort this process. " >> ${LOG_FILE}

      # Inform the team that the two identical process are running

      (echo "From: NM_RADIUS_REPORT\nTo: ${NM_NOTIFICATION_EMAIL_ADDRESS}\n"\
      ; echo "Subject: RADIUS - Flag file problem for ${FLAG_FILE}\n"\
      ; echo "Mime-Version: 1.0\nContent-Type: text/plain\n\n"\
      ; echo "The process identified with the file ${FLAG_FILE} is blocked") |\
      $SENDMAIL_ -t

      exit 1

   fi
fi

# Put pid in the flag_file

echo $$ > "${FLAG_FILE}"
`sleep 2`



###############################################################################
#
#   Setup private variables:
#
LOCAL_CLASSPATH="-classpath ${NM_CLASSPATH}"

SIMULATOR_MAIN_CLASS="com.cgi.nm.util.testTools.$CLASS_NAME"

LOCAL_JAVA_OPTIONS="-Dlog4j.configuration=log4j.properties.non_nm \
                    -Dlog4j.appender.logFile.File=${REPORT_LOG} "

COMMAND_LINE_PARAMS="$PARAM_ACCT_TOS $PARAM_REPORT_DATE"

JAVA_COMMAND="${NM_JAVA_HOME}/bin/java -Xmx2048m -XX:-UseGCOverheadLimit $LOCAL_JAVA_OPTIONS $LOCAL_CLASSPATH $SIMULATOR_MAIN_CLASS $COMMAND_LINE_PARAMS"

###############################################################################
#
#   Execute the java command
#

${JAVA_COMMAND}

echo " "
echo "script $(basename ${0}) ${@} has completed on `date`"
echo " "

###############################################################################
#
#   Remove flag file
#
rm $FLAG_FILE

###############################################################################
#
#   Log end time
#
echo "$(basename ${0}) ${@} script ended on `date`" >> $LOG_FILE