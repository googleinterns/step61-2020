const CLIENT_ID =
    '499747085593-hvi6n4kdrbbfvcuo1c9a9tu9oaf62cr2.apps.googleusercontent.com';

var GoogleAuth; 

// Constants for discovery documents' URLs. 
const DISCOVERY_DOCS_CALENDAR =
    'https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest';

const DISCOVERY_DOCS_SHEETS =
    'https://sheets.googleapis.com/$discovery/rest?version=v4';

const DISCOVERY_DOCS_TASKS =
    'https://www.googleapis.com/discovery/v1/apis/tasks/v1/rest';

// Scopes for API access
const SCOPE_CALENDAR_READ_ONLY = 'https://www.googleapis.com/auth/calendar.readonly';
const SCOPE_CALENDAR_READ_WRITE = 'https://www.googleapis.com/auth/calendar';
const SCOPE_SHEETS_READ_WRITE = 'https://www.googleapis.com/auth/spreadsheets';
const SCOPE_TASKS_READ_ONLY = 'https://www.googleapis.com/auth/tasks.readonly';

// Object for all error codes
const ERROR_CODES = {
  popup_closed_by_user: 'popup_closed_by_user',
  access_denied: 'access_denied'
};

function fetchApiKey() {
  return fetch('./appConfigServlet').then(response => response.json());
}

/**
 * Loads the API's client and auth2 modules.
 * Calls the initCalendarClient function after the modules load.
 */
function handleClientLoad() {
  gapi.load('client:auth2', initAuthentication);
}

function initAuthentication() {
  fetchApiKey().then(responseJson =>
      gapi.client
          .init({
            apiKey: responseJson['API_KEY'],
            clientId: CLIENT_ID,
            discoveryDocs: [DISCOVERY_DOCS_CALENDAR, DISCOVERY_DOCS_SHEETS, DISCOVERY_DOCS_TASKS],
            scope: SCOPE_CALENDAR_READ_ONLY + ' ' 
                + SCOPE_SHEETS_READ_WRITE + ' ' 
                + SCOPE_TASKS_READ_ONLY
          })
          .then( function() {
            GoogleAuth = gapi.auth2.getAuthInstance();

            // Listen for sign-in state changes.
            GoogleAuth.isSignedIn.listen(updateSigninStatus);

            // Handle initial sign-in state. (Determine if user
            // is already signed in.)
            updateSigninStatus(GoogleAuth.isSignedIn.get());
          }));
}

/** Signs user in. */
function handleAuthClick() {
  if (GoogleAuth.isSignedIn.get()) {
    // User is authorized and has clicked "Sign out" button.
    GoogleAuth.signOut();
  } else {
    // User is not signed in. Start Google auth flow.
    GoogleAuth.signIn().catch(error => {
      handleImportAuthError(error);
    });
  }
/*
  GoogleAuth.signIn()
      .then((response) => {
        var $importCalendarMessage = $('#import-calendar-message');
        $importCalendarMessage.addClass('d-none');
        handleApiButtons(); 
        updateCalendarView(); 
      })
      .catch(function(error) {
        handleImportAuthError(error);
      });*/
}

function updateSigninStatus(isSignedIn) {
  if (isSignedIn) {
    var $importCalendarMessage = $('#import-calendar-message');
    $importCalendarMessage.addClass('d-none');
    handleApiButtons();
    handleUiSheets();
    updateCalendarView();
  } else {
    logOutAllApis();
  }
}

/**
 * If current use is signed in and authorized,
 * for read-only access to any of the APIs, 
 * hide the log-in button and show log-out button for that API.
 * Otherwise, the user needs to log in and/or authorize. 
 */
function handleApiButtons() {
  const $logInCalendarButton = $('#calendar-auth-button');
  const $logOutCalendarButton = $('#calendar-logout-button');
  const $exportSheetsButton = $('#sheets-export-button'); 
  const $connectTasksButton = $('#connect-tasks-btn'); 
  const $importCalendarButton = $('#import-calendar-button'); 
  const $exportCalendarButton = $('#export-calendar-button'); 

  if (GoogleAuth && GoogleAuth.isSignedIn.get()) {
    currentUser = GoogleAuth.currentUser.get(); 
    if (currentUser.hasGrantedScopes(SCOPE_CALENDAR_READ_ONLY) ||
        currentUser.hasGrantedScopes(SCOPE_CALENDAR_READ_WRITE)){
      $logInCalendarButton.addClass('d-none');
      $logOutCalendarButton.removeClass('d-none');
      $importCalendarButton .removeClass('d-none');
      $exportCalendarButton.removeClass('d-none');
    } else {
      $logInCalendarButton.removeClass('d-none');
      $logOutCalendarButton.addClass('d-none'); 
      $importCalendarButton .addClass('d-none');
      $exportCalendarButton.addClass('d-none');
    }
      
    if (currentUser.hasGrantedScopes(SCOPE_SHEETS_READ_WRITE)) {
      $exportSheetsButton.removeClass('d-none'); 
    } else {
      $exportSheetsButton.addClass('d-none');
    }

    if (currentUser.hasGrantedScopes(SCOPE_TASKS_READ_ONLY)) {
      $connectTasksButton.removeClass('d-none');
    } else {
      $connectTasksButton.addClass('d-none');
    }
    
  } 
}

/**
 * Handles signing out of the user's Google account.
 * Hides all API buttons accordinly, except the main "Log into your Google account" button.
 */
function logOutAllApis() {
  const $calendarView = $('#calendar-view');
  const $logInCalendarButton = $('#calendar-auth-button');
  const $logOutCalendarButton = $('#calendar-logout-button');
  const $exportSheetsButton = $('#sheets-export-button'); 
  const $connectTasksButton = $('#connect-tasks-btn'); 
  const $importCalendarButton = $('#import-calendar-button');
  const $exportCalendarButton = $('#export-calendar-button'); 

  $calendarView.addClass('d-none');
  $logInCalendarButton.removeClass('d-none');
  $logOutCalendarButton.addClass('d-none');
  $exportCalendarButton.addClass('d-none'); 
  $exportSheetsButton.addClass('d-none');
  $connectTasksButton.addClass('d-none'); 
  $importCalendarButton.addClass('d-none'); 

  clearImportMenu();

  GoogleAuth.signOut();
}