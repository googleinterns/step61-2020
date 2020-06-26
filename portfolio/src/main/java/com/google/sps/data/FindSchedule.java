// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FindSchedule {
  private static final Comparator<CalendarEvent> sortAscending =
      Comparator.comparingLong(CalendarEvent::getStartTimeLong);

  private static final Comparator<Task> sortByDuration =
      Comparator.comparing(Task::getDurationSeconds).thenComparing(Task::getName);

  /**
   * This method returns a Task Collection which the greedy algorithm
   * determines should be scheduled for the person.
   */
  public Collection<Task> greedy(Collection<CalendarEvent> events, Collection<Task> tasks,
      long startTimeMinutes, long endTimeMinutes) {
    List<CalendarEvent> eventsList = new ArrayList<CalendarEvent>(events);
    Task[] tasksArray = tasks.toArray(new Task[tasks.size()]);
    Collections.sort(eventsList, sortAscending);
    Arrays.sort(tasksArray, 0, tasks.size(), sortByDuration);
    TimeRange[] availableTimes = emptyTimeRanges(eventsList, startTimeMinutes, endTimeMinutes);

    List<Task> scheduledTasks = new ArrayList<Task>();
    // Index of TimeRange we are in
    int i = 0;
    // Index of Task we are trying to schedule
    int j = 0;
    // Integer indicating the time (in minutes) we are currently trying to
    // schedule events in
    long time = 0;

    // This will iterate through the time ranges and tasks and if one can be
    // scheduled then it will be (and we move onto the next task) otherwise
    // we move on to the next range (this is because the tasks are sorted by
    // duration so if one task did not fit in the given range then we know no
    // later ones will fit either). We create new Task objects for the result
    // so data structures passed in are never changed.
    while (i < availableTimes.length && j < tasksArray.length) {
      TimeRange timeRange = availableTimes[i];
      Task task = tasksArray[j];
      // Either time is already past the start of the time range or we should
      // update it (maybe this is our first iteration in the range).
      time = Math.max(time, timeRange.start());
      // The task can be scheduled in the current time range.
      if (time + task.getDurationSeconds() <= timeRange.end()) {
        Task scheduledTask;
        if (task.getDescription().isPresent()) {
          scheduledTask =
              new Task(task.getName(), task.getDescription().get(), task.getDuration().toMinutes(),
                  task.getPriority(), Instant.ofEpochSecond(time).toString());
        } else {
          scheduledTask = new Task(task.getName(), null, task.getDuration().toMinutes(),
              task.getPriority(), Instant.ofEpochSecond(time).toString());
        }

        scheduledTasks.add(scheduledTask);
        time = time + task.getDurationSeconds();
        j++;
      } else {
        i++;
      }
    }
    return scheduledTasks;
  }

  /**
   * This method returns an TimeRange array which represent the periods of
   * time that are empty of events and lie completely inside the person's
   * working hours.
   */
  public static TimeRange[] emptyTimeRanges(
      List<CalendarEvent> events, long startTimeMinutes, long endTimeMinutes) {
    List<TimeRange> possibleTimes = new ArrayList<TimeRange>();
    // This represents the earliest time that we can schedule a window for the
    // meeting. As events are processed, this changes to their end times.
    long earliestPossibleSoFar = startTimeMinutes;
    for (CalendarEvent event : events) {
      // Make sure that there is some time between the events and is it not
      // later than the person's working hours ending time.
      if (event.getStartTimeLong() - earliestPossibleSoFar > (long) 0
          && event.getStartTimeLong() <= endTimeMinutes) {
        possibleTimes.add(TimeRange.fromStartEnd(
            earliestPossibleSoFar, event.getStartTimeLong(), /* inclusive= */ true));
      }
      earliestPossibleSoFar = Math.max(earliestPossibleSoFar, event.getEndTimeLong());
    }
    // The end of the day is potentially never included so we check.
    if (endTimeMinutes - earliestPossibleSoFar > 0) {
      possibleTimes.add(
          TimeRange.fromStartEnd(earliestPossibleSoFar, endTimeMinutes, /* inclusive= */ true));
    }
    return possibleTimes.toArray(new TimeRange[possibleTimes.size()]);
  }
}
