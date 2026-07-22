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

import React, { useEffect, useRef } from "react";

import {
  DOCSPACE_OAUTH_CALLBACK_MESSAGE_TYPE,
  DocspaceOAuthCallbackMessage,
} from "../../types/docspace";

const DocspaceOAuthCallbackPage: React.FC = () => {
  const hasRunRef = useRef(false);

  useEffect(() => {
    if (hasRunRef.current) {
      return;
    }
    hasRunRef.current = true;

    const params = new URLSearchParams(window.location.search);

    const message: DocspaceOAuthCallbackMessage = {
      type: DOCSPACE_OAUTH_CALLBACK_MESSAGE_TYPE,
      code: params.get("code") ?? undefined,
      state: params.get("state") ?? undefined,
      error: params.get("error") ?? undefined,
    };

    if (window.opener) {
      window.opener.postMessage(message, window.location.origin);
    }

    window.close();
  }, []);

  return null;
};

export default DocspaceOAuthCallbackPage;
