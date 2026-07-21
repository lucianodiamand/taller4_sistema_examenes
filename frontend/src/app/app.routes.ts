import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'app/student',
    loadComponent: () =>
      import('./student/student-exams.component').then(
        (component) => component.StudentExamsComponent
      )
  },
  { path: '', pathMatch: 'full', redirectTo: 'app/student' },
  { path: '**', redirectTo: 'app/student' }
];
