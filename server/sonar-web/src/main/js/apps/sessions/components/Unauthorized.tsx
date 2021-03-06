/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import * as React from 'react';
import { Link } from 'react-router';

interface Props {
  location: {
    query: {
      message: string;
    };
  };
}

export default function Unauthorized(props: Props) {
  const { message } = props.location.query;

  return (
    <div className="text-center">
      <p id="unauthorized">
        {"You're not authorized to access this page. Please contact the administrator."}
      </p>

      {!!message &&
        <p className="spacer-top">
          Reason : {message}
        </p>}

      <div className="big-spacer-top">
        <Link to="/">Home</Link>
      </div>
    </div>
  );
}
