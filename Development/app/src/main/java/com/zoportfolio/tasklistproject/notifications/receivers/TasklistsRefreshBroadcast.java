package com.zoportfolio.tasklistproject.notifications.receivers;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.zoportfolio.tasklistproject.MainActivity;
import com.zoportfolio.tasklistproject.R;
import com.zoportfolio.tasklistproject.contracts.PublicContracts;
import com.zoportfolio.tasklistproject.tasklist.dataModels.UserTask;
import com.zoportfolio.tasklistproject.tasklist.dataModels.UserTaskList;
import com.zoportfolio.tasklistproject.utility.IOUtility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

public class TasklistsRefreshBroadcast extends BroadcastReceiver {

    private static final String TAG = "TlRefreshBroadcast.TAG";

    private ArrayList<UserTaskList> mTaskLists;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            if(intent.getAction() != null && intent.getAction().equals(PublicContracts.ACTION_RESET_TASKLISTS_BROADCAST)) {

                mTaskLists = IOUtility.loadTasklistsFromStorage(context);
                if(!mTaskLists.isEmpty()) {
                    resetAllTasksToUnchecked(context);
                    setAllTasksAlarm(context);
                }
            }
        }
    }

    private void resetAllTasksToUnchecked(Context _context) {
        for (int i = 0; i < mTaskLists.size(); i++) {
            //Tasklist scope
            if(!mTaskLists.get(i).getTasks().isEmpty()) {
                for (int j = 0; j < mTaskLists.get(i).getTasks().size(); j++) {
                    //Task scope
                    mTaskLists.get(i).getTasks().get(j).setTaskChecked(false);
                }
            }
        }
        IOUtility.saveTasklistsToStorage(_context, mTaskLists);
    }

    private void setAllTasksAlarm(Context _context) {
        for (int i = 0; i < mTaskLists.size(); i++) {
            //Tasklist scope
            if(!mTaskLists.get(i).getTasks().isEmpty()) {
                for (int j = 0; j < mTaskLists.get(i).getTasks().size(); j++) {
                    //Task scope
                    UserTask task = mTaskLists.get(i).getTasks().get(j);
                    setAlarmForTask(_context, task, task.getTaskId());
                }
            }
        }
    }

    private void setAlarmForTask(Context _context, UserTask _task, int _ID) {
        AlarmManager taskAlarmManager = (AlarmManager) _context.getSystemService(Context.ALARM_SERVICE);

        //IMPORTANT, Had to convert the task data into byte data in order to get this to work properly.
        // Filling the intent with the byte array of the task data,
        // implementing SerializationUtils from Apache commons lang3,
        // and adding compileOptions to utilize java 1.8 in gradle.
        Intent taskIntent = new Intent(_context, TaskReminderBroadcast.class);

        byte[] userTaskByteData = UserTask.serializeUserTask(_task);
        taskIntent.putExtra(PublicContracts.EXTRA_TASK_BYTEDATA, userTaskByteData);

        PendingIntent taskAlarmIntent = PendingIntent.getBroadcast(_context.getApplicationContext(),
                _ID,
                taskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if(taskAlarmManager != null) {

            String notificationTime = _task.getTaskNotificationTime();
            String[] notificationTimeSplit = notificationTime.split("/");

            String hour = notificationTimeSplit[0];
            String minute = notificationTimeSplit[1];

            Calendar taskAlarmTime = Calendar.getInstance();
            taskAlarmTime.setTimeInMillis(System.currentTimeMillis());
            taskAlarmTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
            taskAlarmTime.set(Calendar.MINUTE, Integer.parseInt(minute));
            taskAlarmTime.set(Calendar.SECOND, 0);

            taskAlarmManager.set(AlarmManager.RTC_WAKEUP,
                    taskAlarmTime.getTimeInMillis(),
                    taskAlarmIntent);
        }
    }

    //IMPORTANT This is only for testing and will be deleted after beta phase.
//    private void createDebugNotification(Context _context) {
//        //Create the string messages for the notification.
//
//        String bigTextMessage = "This is a test of the tasklistsrefreshBroadcast class, to see if it was activated on bootup or at 12 am midnight.";
//        String bigContentTitleMessage = "All tasks alarmManagers were reset successfully";
//        String summaryTextMessage = "TasklistsRefreshBroadcast class was activated";
//
//        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
//                .bigText(bigTextMessage)
//                .setBigContentTitle(bigContentTitleMessage)
//                .setSummaryText(summaryTextMessage);
//
//        Bitmap largeIcon = BitmapFactory.decodeResource(_context.getResources(), R.drawable.ic_task_notification_placeholder);
//        Notification notification = new NotificationCompat.Builder(_context, MainActivity.NOTIFICATION_CHANNELID_TASKREMINDER)
//                .setLargeIcon(largeIcon)
//                .setSmallIcon(R.drawable.ic_task_notification_placeholder)
//                .setContentTitle("DEBUG NOTIFICATION")
//                .setStyle(bigTextStyle)
//                .build();
//        int notificationId = UUID.randomUUID().hashCode();
//
//        NotificationManager notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
//        if(notificationManager != null) {
//            notificationManager.notify(notificationId, notification);
//        }
//
//    }

}
