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

import { AxiosError } from "axios";

import { ErrorResponse } from "src/types/error";

const PIPEDRIVE_UNAUTHORIZED_CAUSES = new Set([
  "PipedriveAuthenticationRequired",
  "PipedriveOAuth2AuthorizationException",
  "PipedriveWebClientResponseException",
]);

const DOCSPACE_UNAUTHORIZED_CAUSES = new Set([
  "DocspaceOAuth2AuthorizationException",
]);

export const getErrorCause = (e: unknown): string | undefined =>
  e instanceof AxiosError
    ? (e.response?.data as ErrorResponse | undefined)?.cause
    : undefined;

// A 401 without a recognized cause is treated as a Pipedrive auth failure -
// this is the same default the app already relied on, made explicit.
export const isPipedriveUnauthorized = (e: unknown): boolean => {
  if (!(e instanceof AxiosError) || e.response?.status !== 401) {
    return false;
  }

  const cause = getErrorCause(e);
  return !cause || PIPEDRIVE_UNAUTHORIZED_CAUSES.has(cause);
};

export const isDocspaceUnauthorized = (e: unknown): boolean =>
  e instanceof AxiosError &&
  e.response?.status === 401 &&
  DOCSPACE_UNAUTHORIZED_CAUSES.has(getErrorCause(e) ?? "");
