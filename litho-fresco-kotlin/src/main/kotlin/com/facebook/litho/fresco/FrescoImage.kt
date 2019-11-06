/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
 */

package com.facebook.litho.fresco

import com.facebook.drawee.interfaces.DraweeController
import com.facebook.litho.ComponentContext

/**
 * Builder function for creating [FrescoImageSpec] components.
 */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ComponentContext.FrescoImage(
    controller: DraweeController,
    imageAspectRatio: Float
): FrescoImage.Builder =
    FrescoImage.create(this)
        .controller(controller)
        .imageAspectRatio(imageAspectRatio)
