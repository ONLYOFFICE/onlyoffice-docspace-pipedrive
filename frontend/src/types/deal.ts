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

export type Deal = {
  id: number;
  title: string;
  status: string;
};

export type DealSearchItem = {
  result_score: number;
  item: Deal;
};

export type DealFile = {
  id: number;
  file_name: string;
  file_size: number;
  file_type: string;
  url: string;
  deal_id: number;
  add_time: string;
};

export type DealsResponse = {
  success: boolean;
  data: Deal[];
  additional_data: {
    next_cursor: string | null;
  };
};

export type DealSearchResponse = {
  success: boolean;
  data: DealSearchItem[];
  additional_data: {
    next_cursor: string | null;
  };
};

export type DealFilesResponse = {
  success: boolean;
  data: DealFile[];
  additional_data: {
    pagination: {
      start: number;
      limit: number;
      more_items_in_collection: boolean;
      next_start?: number;
    };
  };
};
