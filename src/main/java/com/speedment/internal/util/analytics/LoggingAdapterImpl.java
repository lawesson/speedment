/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.internal.util.analytics;

import com.speedment.internal.logging.Logger;
import com.speedment.internal.logging.LoggerManager;


/**
 *
 * @author pemi
 */
public final class LoggingAdapterImpl implements LoggingAdapter {

    private final static Logger LOGGER = LoggerManager.getLogger(LoggingAdapterImpl.class);

    @Override
    public void logError(String errorMessage) {
        LOGGER.error(errorMessage);
    }

    @Override
    public void logMessage(String message) {
        LOGGER.info(message);
    }

}
