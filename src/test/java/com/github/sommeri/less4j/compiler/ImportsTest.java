package com.github.sommeri.less4j.compiler;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import com.github.sommeri.less4j.utils.TestFileUtils;

//TODO: create issue: import-types-combinations.less must be revisited under 1.4.0
public class ImportsTest extends AbstractErrorReportingTest {

  private static final String standardCases = "src/test/resources/compile-basic-features/import/";

  public ImportsTest(File lessFile, File cssOutput, File errorList, String testName) {
    super(lessFile, cssOutput, errorList, testName);
  }

  @Parameters()
  public static Collection<Object[]> allTestsParameters() {
    return (new TestFileUtils(".err")).loadTestFiles(standardCases);
  }

}
