import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { MainLayout } from '../layouts/MainLayout';
import { DashboardPage } from '../pages/DashboardPage';
import { UploadDocumentPage } from '../pages/UploadDocumentPage';
import { MySubmissionsPage } from '../pages/MySubmissionsPage';
import { ValidationResultsPage } from '../pages/ValidationResultsPage';
import { HelpPage } from '../pages/HelpPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/upload" element={<UploadDocumentPage />} />
        <Route path="/submissions" element={<MySubmissionsPage />} />
        <Route path="/results" element={<ValidationResultsPage />} />
        <Route path="/results/:id" element={<ValidationResultsPage />} />
        <Route path="/help" element={<HelpPage />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Route>
    </Routes>
  );
};
