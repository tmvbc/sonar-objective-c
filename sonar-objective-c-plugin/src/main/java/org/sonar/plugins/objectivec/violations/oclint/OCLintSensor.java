/**
 * backelite-sonar-objective-c-plugin - Enables analysis of Objective-C projects into SonarQube.
 * Copyright © 2012 OCTO Technology, Backelite (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sonar.plugins.objectivec.violations.oclint;

import java.io.File;

import org.apache.tools.ant.DirectoryScanner;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.plugins.objectivec.ObjectiveCPlugin;
import org.sonar.plugins.objectivec.core.ObjectiveC;

public final class OCLintSensor implements Sensor {
    public static final String REPORT_PATH_KEY = ObjectiveCPlugin.PROPERTY_PREFIX + ".oclint.report";
    public static final String DEFAULT_REPORT_PATH = "sonar-reports/*oclint.xml";

    private final Settings conf;
    private final FileSystem fileSystem;
    private final ResourcePerspectives resourcePerspectives;

    public OCLintSensor(final FileSystem fileSystem, final Settings config, final ResourcePerspectives resourcePerspectives) {
        this.conf = config;
        this.fileSystem = fileSystem;
        this.resourcePerspectives = resourcePerspectives;
    }

    public boolean shouldExecuteOnProject(final Project project) {

        return project.isRoot() && fileSystem.languages().contains(ObjectiveC.KEY);

    }

    public void analyse(final Project project, final SensorContext context) {
        final String projectBaseDir = fileSystem.baseDir().getPath();
        final OCLintParser parser = new OCLintParser(project, context, resourcePerspectives, fileSystem);

        parseReportIn(projectBaseDir, parser);

    }

    private void parseReportIn(final String baseDir, final OCLintParser parser) {

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{reportPath()});
        scanner.setBasedir(baseDir);
        scanner.setCaseSensitive(false);
        scanner.scan();
        String[] files = scanner.getIncludedFiles();

        for(String filename : files) {
            LoggerFactory.getLogger(getClass()).info("Processing OCLint report {}", filename);
            parser.parseReport(new File(filename));
        }
    }

    private String reportPath() {
        String reportPath = conf.getString(REPORT_PATH_KEY);
        if (reportPath == null) {
            reportPath = DEFAULT_REPORT_PATH;
        }
        return reportPath;
    }
}
