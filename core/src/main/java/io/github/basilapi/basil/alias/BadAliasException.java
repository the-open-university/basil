/*
 * Copyright (c) 2021. Enrico Daga and Luca Panziera
 *
 * MLicensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.github.basilapi.basil.alias;

import java.io.IOException;

public class BadAliasException extends IOException {
	private String alias;

	private static final long serialVersionUID = 1L;

	public BadAliasException(String alias) {
		this.alias = alias;
	}

	@Override
	public String getMessage() {
		return "The alias provided does not respects the pattern: " + AliasUtils.REGEX.pattern();
	}

	public String getBadAlias() {
		return alias;
	}
}
