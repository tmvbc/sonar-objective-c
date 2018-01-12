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
package org.sonar.plugins.objectivec.complexity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.plugins.objectivec.ObjectiveCPlugin;
import org.sonar.plugins.objectivec.core.ObjectiveC;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * This sensor searches for the report generated from the tool Lizard
 * in order to save complexity metrics.
 *
 * @author Andres Gil Herrera
 * @since 28/05/15
 */
public class LizardSensor implements Sensor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LizardSensor.class);

    public static final String REPORT_PATH_KEY = ObjectiveCPlugin.PROPERTY_PREFIX + ".lizard.report";
    public static final String DEFAULT_REPORT_PATH = "sonar-reports/lizard-report.xml";

    private final Settings conf;
    private final FileSystem fileSystem;

    public LizardSensor(final FileSystem moduleFileSystem, final Settings config) {
        this.conf = config;
        this.fileSystem = moduleFileSystem;
    }

    /**
     *
     * @param project
     * @return true if the project is root the root project and uses Objective-C
     */
    public boolean shouldExecuteOnProject(Project project) {
        return project.isRoot() && fileSystem.languages().contains(ObjectiveC.KEY);
    }

    /**
     *
     * @param project
     * @param sensorContext
     */
    public void analyse(Project project, SensorContext sensorContext) {
        final String projectBaseDir = fileSystem.baseDir().getPath();
        Map<String, List<Measure>> measures = parseReportsIn(projectBaseDir, new LizardReportParser());
        LOGGER.info("Saving results of complexity analysis");
        new LizardMeasurePersistor(project, sensorContext, fileSystem).saveMeasures(measures);
    }

    /**
     *
     * @param baseDir base directory of the project to search the report
     * @param parser LizardReportParser to parse the report
     * @return Map containing as key the name of the file and as value a list containing the measures for that file
     */
    private Map<String, List<Measure>> parseReportsIn(final String baseDir, LizardReportParser parser) {
        final StringBuilder reportFileName = new StringBuilder(baseDir);
        reportFileName.append("/").append(reportPath());
        LOGGER.info("Processing complexity report ");
        return parser.parseReport(new File(reportFileName.toString()));
    }

    /**
     *
     * @return the default report path or the one specified in the sonar-project.properties
     */
    private String reportPath() {
        String reportPath = conf.getString(REPORT_PATH_KEY);
        if (reportPath == null) {
            reportPath = DEFAULT_REPORT_PATH;
        }
        return reportPath;
    }
}