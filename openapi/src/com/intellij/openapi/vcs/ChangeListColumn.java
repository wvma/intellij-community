/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 27.11.2006
 * Time: 20:22:50
 */
package com.intellij.openapi.vcs;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.util.Comparator;

public abstract class ChangeListColumn<T extends ChangeList> {
  public abstract String getTitle();
  public abstract Object getValue(T changeList);

  @Nullable
  public Comparator<T> getComparator() {
    return null;
  }

  public static ChangeListColumn<CommittedChangeList> DATE = new ChangeListColumn<CommittedChangeList>() {
    public String getTitle() {
      return VcsBundle.message("column.name.revision.list.date");
    }

    public Object getValue(final CommittedChangeList changeList) {
      return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(changeList.getCommitDate());
    }

    public Comparator<CommittedChangeList> getComparator() {
      return new Comparator<CommittedChangeList>() {
        public int compare(final CommittedChangeList o1, final CommittedChangeList o2) {
          return o1.getCommitDate().compareTo(o2.getCommitDate());
        }
      };
    }
  };

  public static ChangeListColumn<CommittedChangeList> NAME = new ChangeListColumn<CommittedChangeList>() {
    public String getTitle() {
      return VcsBundle.message("column.name.revision.list.committer");
    }

    public Object getValue(final CommittedChangeList changeList) {
      return changeList.getCommitterName();
    }

    public Comparator<CommittedChangeList> getComparator() {
      return new Comparator<CommittedChangeList>() {
        public int compare(final CommittedChangeList o1, final CommittedChangeList o2) {
          return Comparing.compare((String) getValue(o1), (String) getValue(o2));
        }
      };
    }
  };

  public static ChangeListColumn<CommittedChangeList> NUMBER = new ChangeListColumn<CommittedChangeList>() {
    public String getTitle() {
      return VcsBundle.message("column.name.revision.list.number");
    }

    public Object getValue(final CommittedChangeList changeList) {
      return changeList.getNumber();
    }

    public Comparator<CommittedChangeList> getComparator() {
      return new Comparator<CommittedChangeList>() {
        public int compare(final CommittedChangeList o1, final CommittedChangeList o2) {
          return (int)(o1.getNumber() - o2.getNumber());
        }
      };
    }
  };

  public static ChangeListColumn<CommittedChangeList> DESCRIPTION = new ChangeListColumn<CommittedChangeList>() {
    public String getTitle() {
      return VcsBundle.message("column.name.revision.list.description");
    }

    public Object getValue(final CommittedChangeList changeList) {
      return changeList.getName();
    }

    public Comparator<CommittedChangeList> getComparator() {
      return new Comparator<CommittedChangeList>() {
        public int compare(final CommittedChangeList o1, final CommittedChangeList o2) {
          return o1.getName().compareTo(o2.getName());
        }
      };
    }
  };
}