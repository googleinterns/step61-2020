package com.google.sps.data;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ShortestTaskFirstTest {
  private static Instant BEGINNING_OF_DAY = Instant.parse("2020-06-25T00:00:00Z");
  private static Instant END_OF_DAY = Instant.parse("2020-06-25T23:59:59Z");
  private static Instant THREE_DAYS_LATER = Instant.parse("2020-06-28T00:00:00Z");

  private static Duration TIME_15_MINUTES = Duration.ofSeconds(900);
  private static Duration TIME_20_MINUTES = Duration.ofSeconds(1200);
  private static Duration TIME_30_MINUTES = Duration.ofSeconds(1800);
  private static Duration TIME_45_MINUTES = Duration.ofSeconds(2700);
  private static Duration TIME_60_MINUTES = Duration.ofSeconds(3600);

  private static Instant TIME_0900 = Instant.parse("2020-06-25T09:00:00Z");
  private static Instant TIME_0920 = TIME_0900.plus(TIME_20_MINUTES);
  private static Instant TIME_0950 = TIME_0900.plus(Duration.ofSeconds(3000));
  private static Instant TIME_1000 = TIME_0900.plus(TIME_60_MINUTES);
  private static Instant TIME_1020 = TIME_1000.plus(TIME_20_MINUTES);
  private static Instant TIME_1030 = TIME_1000.plus(TIME_30_MINUTES);
  private static Instant TIME_1100 = TIME_1000.plus(TIME_60_MINUTES);
  private static Instant TIME_1200 = TIME_1100.plus(TIME_60_MINUTES);
  private static Instant TIME_1300 = TIME_1200.plus(TIME_60_MINUTES);
  private static Instant TIME_1400 = TIME_1300.plus(TIME_60_MINUTES);
  private static Instant TIME_1500 = TIME_1400.plus(TIME_60_MINUTES);
  private static Instant TIME_1600 = TIME_1500.plus(TIME_60_MINUTES);
  private static Instant TIME_1700 = TIME_1600.plus(TIME_60_MINUTES);
  private static Instant TIME_1800 = TIME_1700.plus(TIME_60_MINUTES);

  private final TaskPriority LOW_PRIORITY = new TaskPriority(1);

  // Makes sure we return an empty list in the case where no tasks are passed.
  @Test
  public void noTasksScheduled() {
    ShortestTaskFirst algorithm = new ShortestTaskFirst();

    Collection<CalendarEvent> events =
        Arrays.asList(new CalendarEvent("Event 1", BEGINNING_OF_DAY, END_OF_DAY));
    Collection<Task> tasks = Arrays.asList();

    Collection<ScheduledTask> actual = algorithm.schedule(events, tasks, TIME_0900, TIME_1700);
    Collection<ScheduledTask> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }

  // Makes sure no errors are thrown in the case where no events are passed and
  // we schedule the tasks. It also checks that we begin scheduling after the
  // proposed startTime.
  @Test
  public void noEventsScheduled() {
    ShortestTaskFirst algorithm = new ShortestTaskFirst();

    Collection<CalendarEvent> events = Arrays.asList();
    Task task1 = new Task("Task 1", "First task", TIME_30_MINUTES, LOW_PRIORITY);
    Collection<Task> tasks = Arrays.asList(task1);
    ScheduledTask scheduledTask1 = new ScheduledTask(task1, TIME_0900);

    Collection<ScheduledTask> actual = algorithm.schedule(events, tasks, TIME_0900, TIME_1700);
    Collection<ScheduledTask> expected = Arrays.asList(scheduledTask1);
    Assert.assertEquals(expected, actual);
  }

  // Makes sure that tasks are being scheduled from shortest to longest.
  @Test
  public void sortingTasksCorrectly() {
    ShortestTaskFirst algorithm = new ShortestTaskFirst();

    Collection<CalendarEvent> events = Arrays.asList();
    Task task1 = new Task("Task 1", "First task", TIME_30_MINUTES, LOW_PRIORITY);
    Task task2 = new Task("Task 2", "Second task", TIME_30_MINUTES, LOW_PRIORITY);
    Task task3 = new Task("Task 3", "Third task", TIME_20_MINUTES, LOW_PRIORITY);
    Task task4 = new Task("Task 4", "Fourth task", TIME_45_MINUTES, LOW_PRIORITY);
    Collection<Task> tasks = Arrays.asList(task1, task2, task3, task4);
    // Scheduled tasks throughout the file are meant to correspond with the
    // same numbered regular task, they are not numbered by their startTime.
    ScheduledTask scheduledTask1 = new ScheduledTask(task1, TIME_0920);
    ScheduledTask scheduledTask2 = new ScheduledTask(task2, TIME_0950);
    ScheduledTask scheduledTask3 = new ScheduledTask(task3, TIME_0900);
    ScheduledTask scheduledTask4 = new ScheduledTask(task4, TIME_1020);

    Collection<ScheduledTask> actual = algorithm.schedule(events, tasks, TIME_0900, TIME_1700);
    Collection<ScheduledTask> expected =
        Arrays.asList(scheduledTask3, scheduledTask1, scheduledTask2, scheduledTask4);
    Assert.assertEquals(expected, actual);
  }

  // Makes sure that tasks are being scheduled only during periods of time that
  // contain no events.
  @Test
  public void schedulingOnlyInAvailableTimes() {
    ShortestTaskFirst algorithm = new ShortestTaskFirst();
    Collection<CalendarEvent> events =
        Arrays.asList(
            new CalendarEvent("Event 1", TIME_0920, TIME_1000),
            new CalendarEvent("Event 2", TIME_1030, TIME_1700));

    Task task1 = new Task("Task 1", "First task", TIME_20_MINUTES, LOW_PRIORITY);
    Task task2 = new Task("Task 2", "Second task", TIME_30_MINUTES, LOW_PRIORITY);
    Collection<Task> tasks = Arrays.asList(task1, task2);
    ScheduledTask scheduledTask1 = new ScheduledTask(task1, TIME_0900);
    ScheduledTask scheduledTask2 = new ScheduledTask(task2, TIME_1000);

    Collection<ScheduledTask> actual = algorithm.schedule(events, tasks, TIME_0900, TIME_1700);
    Collection<ScheduledTask> expected = Arrays.asList(scheduledTask1, scheduledTask2);
    Assert.assertEquals(expected, actual);
  }

  // Makes sure that no errors are thrown for events fully outside working
  // hours.
  @Test
  public void eventTotallyOutsideWorkingHours() {
    ShortestTaskFirst algorithm = new ShortestTaskFirst();

    Collection<CalendarEvent> events =
        Arrays.asList(new CalendarEvent("Event 1", TIME_1800, TIME_2000));
    Task task1 = new Task("Task 1", "First task", TIME_30_MINUTES, LOW_PRIORITY);
    Collection<Task> tasks = Arrays.asList(task1);
    ScheduledTask scheduledTask1 = new ScheduledTask(task1, TIME_0900);
    Collection<ScheduledTask> actual = algorithm.schedule(events, tasks, TIME_0900, TIME_1700);
    Collection<ScheduledTask> expected = Arrays.asList(scheduledTask1);
    Assert.assertEquals(expected, actual);
  }

  // Makes sure that no errors are thrown for events partially outside working
  // hours.
  @Test
  public void eventPartiallyOutsideWorkingHours() {
    ShortestTaskFirst algorithm = new ShortestTaskFirst();

    Collection<CalendarEvent> events =
        Arrays.asList(new CalendarEvent("Event 1", TIME_0830, TIME_0930));
    Task task1 = new Task("Task 1", "First task", TIME_30_MINUTES, LOW_PRIORITY);
    Collection<Task> tasks = Arrays.asList(task1);
    ScheduledTask scheduledTask1 = new ScheduledTask(task1, TIME_0930);
    Collection<ScheduledTask> actual = algorithm.schedule(events, tasks, TIME_0900, TIME_1700);
    Collection<ScheduledTask> expected = Arrays.asList(scheduledTask1);
    Assert.assertEquals(expected, actual);
  }

  // Makes sure that no errors are thrown if working hours is longer than a
  // 24 hour period.
  @Test
  public void workingPeriodExceedsTwentyFourHours() {
    ShortestTaskFirst algorithm = new ShortestTaskFirst();

    Collection<CalendarEvent> events = Arrays.asList();
    Task task1 = new Task("Task 1", "First task", TIME_30_MINUTES, LOW_PRIORITY);
    Collection<Task> tasks = Arrays.asList(task1);
    ScheduledTask scheduledTask1 = new ScheduledTask(task1, TIME_0900);

    Collection<ScheduledTask> actual =
        algorithm.schedule(events, tasks, TIME_0900, THREE_DAYS_LATER);
    Collection<ScheduledTask> expected = Arrays.asList(scheduledTask1);
    Assert.assertEquals(expected, actual);
  }

  // Makes sure no errors are thrown if we have more tasks than we can possibly
  // schedule.
  @Test
  public void tooManyTasks() {
    ShortestTaskFirst algorithm = new ShortestTaskFirst();

    Collection<CalendarEvent> events =
        Arrays.asList(new CalendarEvent("Event 1", TIME_1000, TIME_1700));
    Task task1 = new Task("Task 1", "First task", TIME_30_MINUTES, LOW_PRIORITY);
    Task task2 = new Task("Task 2", "Second task", TIME_30_MINUTES, LOW_PRIORITY);
    Task task3 = new Task("Task 3", "Third task", TIME_20_MINUTES, LOW_PRIORITY);
    Task task4 = new Task("Task 4", "Fourth task", TIME_45_MINUTES, LOW_PRIORITY);
    Collection<Task> tasks = Arrays.asList(task1, task2, task3, task4);
    ScheduledTask scheduledTask1 = new ScheduledTask(task1, TIME_0920);
    ScheduledTask scheduledTask3 = new ScheduledTask(task3, TIME_0900);

    Collection<ScheduledTask> actual = algorithm.schedule(events, tasks, TIME_0900, TIME_1700);
    Collection<ScheduledTask> expected = Arrays.asList(scheduledTask3, scheduledTask1);
    Assert.assertEquals(expected, actual);
  }
}
