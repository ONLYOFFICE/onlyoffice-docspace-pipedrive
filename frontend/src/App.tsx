/**
 *
 * (c) Copyright Ascensio System SIA 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import React from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
  useLocation,
} from "react-router-dom";

import { AppContextProvider } from "@context/AppContext";
import { ErrorPage } from "@pages/Error";

const SettingsPage = React.lazy(() => import("@pages/Settings"));
const RoomPage = React.lazy(() => import("@pages/Room"));
const DocspaceOAuthCallbackPage = React.lazy(
  () => import("@pages/DocspaceOAuthCallback"),
);

const LazyRoutes: React.FC = () => {
  const location = useLocation();
  return (
    <Routes location={location} key={location.pathname}>
      <Route path="/">
        <Route
          path="settings"
          element={
            <AppContextProvider>
              <ErrorPage>
                <SettingsPage />
              </ErrorPage>
            </AppContextProvider>
          }
        />
        <Route
          path="room"
          element={
            <AppContextProvider>
              <ErrorPage>
                <RoomPage />
              </ErrorPage>
            </AppContextProvider>
          }
        />
        <Route
          path="oauth2/docspace/callback"
          element={<DocspaceOAuthCallbackPage />}
        />
      </Route>
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
};

function App() {
  return (
    <div className="w-full h-full font-normal text-pipedrive-color-light-neutral-1000 text-base dark:bg-pipedrive-color-dark-neutral-100 dark:text-pipedrive-color-dark-neutral-1000 flex justify-center items-center">
      <Router>
        <LazyRoutes />
      </Router>
    </div>
  );
}

export default App;
