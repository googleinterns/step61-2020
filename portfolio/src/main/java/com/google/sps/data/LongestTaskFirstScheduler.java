package com.google.sps.data;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** This class models a scheduling algorithm that prioritizes scheduling longer tasks first. */
public class LongestTaskFirstScheduler implements TaskScheduler {

  // Comparator for sorting tasks by duration in descending order and then by task priority
  // descending.
  public static final Comparator<Task> sortByTaskDurationDescendingThenPriority =
      Comparator.comparing(Task::getDuration).reversed().thenComparing(Task::getPriority);

  /**
   * Schedules the tasks so that the longest tasks are scheduled to the first possible free time
   * range of the day. This approach tries to prioritize long tasks so that they are scheduled.
   */
  public Collection<ScheduledTask> schedule(
      Collection<CalendarEvent> events,
      Collection<Task> tasks,
      Instant workHoursStartTime,
      Instant workHoursEndTime) {

    List<CalendarEvent> eventsList = new ArrayList<CalendarEvent>(events);
    List<Task> tasksList = new ArrayList<Task>(tasks);

    // Sorts the tasks in descending order based on duration.
    // Longest task comes first in the collection.
    // Then sorts the list again by task priority.
    Collections.sort(tasksList, sortByTaskDurationDescendingThenPriority);

    CalendarEventsGroup calendarEventsGroup =
        new CalendarEventsGroup(eventsList, workHoursStartTime, workHoursEndTime);

    // Create a TimeRangeGroup class for the free time ranges.
    List<TimeRange> availableTimes = calendarEventsGroup.getFreeTimeRanges();
    TimeRangeGroup availableTimesGroup = new ArrayListTimeRangeGroup(availableTimes);

    List<ScheduledTask> scheduledTasks = new ArrayList<ScheduledTask>();

    for (Task task : tasksList) {
      List<ScheduledTask> currentScheduledTasks = scheduleOneTask(task, availableTimesGroup);

      currentScheduledTasks.forEach(
          (scheduledTask) -> {
            scheduledTasks.add(scheduledTask);
          });
    }

    return scheduledTasks;
  }

  /** Returns the scheduler's type, which is Longest Task First. */
  public SchedulingAlgorithmType getSchedulingAlgorithmType() {
    return SchedulingAlgorithmType.LONGEST_TASK_FIRST;
  }

  /** Constructs a list for the updated available time ranges after deletion. */
  private List<TimeRange> constructAvailableTimeRanges(TimeRangeGroup updatedTimeRangeGroup) {
    Iterator<TimeRange> updatedAvailableTimesGroupIterator = updatedTimeRangeGroup.iterator();
    List<TimeRange> updatedAvailableTimes = new ArrayList();
    while (updatedAvailableTimesGroupIterator.hasNext()) {
      updatedAvailableTimes.add(updatedAvailableTimesGroupIterator.next());
    }

    return updatedAvailableTimes;
  }

  /**
   * Tries to schedule for a single task by iterating through the currently available time ranges.
   * Keeps splitting up the task to schedule the currently avaible free time ranges until all of the
   * task is scheduled across the different time ranges.
   *
   * <p>When this method is called, the task may or may not be complete scheduled. The task may be
   * scheduled into one entire free time range, or the task could be split into multiple time
   * ranges. When a task is split, new tasks are created to model the segment of the current task
   * and these new tasks are added to the list of all scheduled tasks.
   *
   * @return a list of newly scheduled tasks. If this list is empty, then the current task cannot be
   *     scheduled.
   */
  private List<ScheduledTask> scheduleOneTask(Task task, TimeRangeGroup availableTimesGroup) {
    List<TimeRange> currentAvailableTimes = constructAvailableTimeRanges(availableTimesGroup);
    List<ScheduledTask> newScheduledTasks = new ArrayList<ScheduledTask>();

    // If there is no available time ranges anymore,
    // return the empty list.
    if (currentAvailableTimes.isEmpty()) {
      return newScheduledTasks;
    }

    Duration taskDuration = task.getDuration();
    int taskSegmentCount = 1;

    for (TimeRange currentFreeTimeRange : currentAvailableTimes) {
      if (taskDuration.getSeconds() == 0) {
        checkForTaskRenaming(newScheduledTasks);
        return newScheduledTasks;
      }

      Instant scheduledTime = currentFreeTimeRange.start();

      // Reconstruct the task segment's name.
      String taskName = task.getName() + " (Part " + taskSegmentCount + ")";
      taskSegmentCount++;

      String taskDescription = task.getDescription().orElse("");
      TaskPriority taskPriority = task.getPriority();

      // If task's current duration is longer than the free time's,
      // this means the entirety of the free time range is scheduled to this task.
      if (taskDuration.compareTo(currentFreeTimeRange.duration()) > 0) {
        // Constructs a new taks object with the current free time range's duration.
        Task taskWithActualScheduledDuration =
            new Task(taskName, taskDescription, currentFreeTimeRange.duration(), taskPriority);
        TimeRange scheduledTaskTimeRange =
            scheduleTaskSegment(
                taskWithActualScheduledDuration,
                scheduledTime,
                newScheduledTasks,
                availableTimesGroup);

        taskDuration = taskDuration.minus(scheduledTaskTimeRange.duration());
      } else {
        // Otherwise, only part of the free time range is needed to schedule this task.
        Task taskWithActualScheduledDuration =
            new Task(taskName, taskDescription, taskDuration, taskPriority);
        TimeRange scheduledTaskTimeRange =
            scheduleTaskSegment(
                taskWithActualScheduledDuration,
                scheduledTime,
                newScheduledTasks,
                availableTimesGroup);
        taskDuration = taskDuration.minus(scheduledTaskTimeRange.duration());

        // This is also the last free time range that is needed to complete the scheduling
        // for the current task.
        checkForTaskRenaming(newScheduledTasks);
        return newScheduledTasks;
      }
    }
    // If iterating through all current time ranges finishes, and the task still isn't
    // completely scheduled, then this task cannot be completely scheduled.
    // TODO(hollyyuqizheng): add logic for UI updates for the partially scheduled tasks.
    return newScheduledTasks;
  }

  /**
   * Schedules a task segment based on a scheduled time. Deletes the scheduled time range from
   * availabe time range group.
   */
  private TimeRange scheduleTaskSegment(
      Task task,
      Instant scheduledTime,
      List<ScheduledTask> scheduledTasks,
      TimeRangeGroup availableTimesGroup) {
    Duration taskDuration = task.getDuration();

    ScheduledTask scheduledTask = new ScheduledTask(task, scheduledTime);
    scheduledTasks.add(scheduledTask);

    Instant scheduledTaskEndTime = scheduledTime.plus(taskDuration);
    TimeRange scheduledTaskTimeRange = TimeRange.fromStartEnd(scheduledTime, scheduledTaskEndTime);
    availableTimesGroup.deleteTimeRange(scheduledTaskTimeRange);

    return scheduledTaskTimeRange;
  }

  /**
   * This method is called when a task can be completely scheduled before returning that scheduled
   * task. Checks to see if this scheduled task needs to be renamed back to its original name. If
   * there is only one task in the newly scheduled list of tasks, then this task is completely
   * scheduled within one time range, so the name should not include "(Part 1)".
   */
  private void checkForTaskRenaming(List<ScheduledTask> newScheduledTasks) {
    if (newScheduledTasks.size() == 1) {
      ScheduledTask scheduledTask = newScheduledTasks.get(0);
      ScheduledTask scheduledTaskWithOriginalName = changeTaskNameToOriginal(scheduledTask);
      newScheduledTasks.set(0, scheduledTaskWithOriginalName);
    }
  }

  /** Creates a new ScheduledTask with "(Part 1)" removed. */
  static ScheduledTask changeTaskNameToOriginal(ScheduledTask scheduledTask) {
    Task task = scheduledTask.getTask();
    String currentScheduledTaskName = task.getName();
    String taskOriginalName = currentScheduledTaskName.split(" \\(Part ")[0];

    Task taskWithOriginalName =
        new Task(
            taskOriginalName,
            task.getDescription().orElse(""),
            task.getDuration(),
            task.getPriority());

    ScheduledTask scheduledTaskWithNewName =
        new ScheduledTask(taskWithOriginalName, scheduledTask.getStartTime());

    return scheduledTaskWithNewName;
  }
}
