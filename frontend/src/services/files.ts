/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

import axios from "axios";
import axiosRetry from "axios-retry";

import { SendFileRequest } from "src/types/files";
import { PipedriveToken } from "@context/PipedriveToken";

export const sendFromDocspaceToPipedrive = async (
  pipedriveToken: PipedriveToken,
  request: SendFileRequest,
) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });
  axiosRetry(client, {
    retries: 2,
    retryCondition: (error) =>
      axiosRetry.isNetworkOrIdempotentRequestError(error) ||
      (error.response?.status !== undefined && error.response.status >= 500),
    retryDelay: (count) => count * 50,
    shouldResetTimeout: true,
  });

  const response = await client<Record<string, unknown>>({
    method: "POST",
    url: "/api/v1/files/send/from-docspace-to-pipedrive",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    data: request,
    timeout: 60000,
  });

  return response.data;
};

export const sendFromPipedriveToDocspace = async (
  pipedriveToken: PipedriveToken,
  request: SendFileRequest,
) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });
  axiosRetry(client, {
    retries: 2,
    retryCondition: (error) =>
      axiosRetry.isNetworkOrIdempotentRequestError(error) ||
      (error.response?.status !== undefined && error.response.status >= 500),
    retryDelay: (count) => count * 50,
    shouldResetTimeout: true,
  });

  const response = await client<Record<string, unknown>>({
    method: "POST",
    url: "/api/v1/files/send/from-pipedrive-to-docspace",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    data: request,
    timeout: 60000,
  });

  return response.data;
};
