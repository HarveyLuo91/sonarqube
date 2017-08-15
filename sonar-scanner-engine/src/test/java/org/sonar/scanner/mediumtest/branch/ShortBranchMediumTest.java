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
package org.sonar.scanner.mediumtest.branch;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.scan.BranchConfiguration;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class ShortBranchMediumTest {

  private File baseDir;
  final String relativePath = "HelloJava.xoo";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo");

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    Path filepath = baseDir.toPath().resolve(relativePath);
    Files.write(filepath, "xoooo".getBytes());

    String md5sum = new FileMetadata()
      .readMetadata(Files.newInputStream(filepath), StandardCharsets.UTF_8, relativePath)
      .hash();
    tester.addFileData("sample", relativePath, new FileData(md5sum, null));
  }

  @Test
  public void should_skip_report_for_unchanged_file_in_short_branch() {
    // sanity check, normally report gets generated
    TaskResult result = getResult(tester);
    assertThat(getResult(tester).getReportComponent(result.inputFile(relativePath).key())).isNotNull();

    // file is skipped for short branches (no report, no coverage, no duplications)
    TaskResult result2 = getResult(tester.setBranchType(BranchConfiguration.BranchType.SHORT));
    assertThat(result2.getReportComponent(result2.inputFile(relativePath).key())).isNull();
  }

  @Test
  public void verify_metadata() {
    String branchName = "feature";
    String branchTarget = "branch-1.x";

    TaskResult result = getResult(tester
      .setBranchName(branchName)
      .setBranchTarget(branchTarget)
      .setBranchType(BranchConfiguration.BranchType.SHORT));

    ScannerReport.Metadata metadata = result.getReportReader().readMetadata();
    assertThat(metadata.getBranchName()).isEqualTo(branchName);
    assertThat(metadata.getBranchType()).isEqualTo(ScannerReport.Metadata.BranchType.SHORT);
    assertThat(metadata.getMergeBranchName()).isEqualTo(branchTarget);
  }

  private TaskResult getResult(ScannerMediumTester tester) {
    return tester
      .newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "sample")
        .put("sonar.sources", ".")
        .build())
      .execute();
  }
}
