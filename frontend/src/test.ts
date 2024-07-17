// This file is required by karma.conf.js and loads recursively all the .spec and framework files

import 'zone.js/testing';
import {getTestBed} from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting
} from '@angular/platform-browser-dynamic/testing';
import {HttpClientModule} from "@angular/common/http";
import {RouterTestingModule} from "@angular/router/testing";

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
  [BrowserDynamicTestingModule,
    HttpClientModule,
    RouterTestingModule],
  platformBrowserDynamicTesting(),
  {teardown: {destroyAfterEach: true}},
);