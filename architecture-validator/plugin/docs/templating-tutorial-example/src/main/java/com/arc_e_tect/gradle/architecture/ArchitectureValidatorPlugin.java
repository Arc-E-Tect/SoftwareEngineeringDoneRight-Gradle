package com.arc_e_tect.gradle.architecture;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ArchitectureValidatorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        ArchitectureValidatorExtension extension = project.getExtensions().create(
                ArchitectureValidatorExtension.NAME,
                ArchitectureValidatorExtension.class,
                project.getObjects(),
                project.getLayout());

        project.getTasks().register("generateArchitectureTests", GenerateArchitectureTestsTask.class, task -> {
            task.getBasePackage().set(extension.getBasePackage());
            task.getBuiltInTemplate().set(extension.getBuiltInTemplate());

            task.getInPorts().set(extension.getHexagonalArchitecture().getInPorts());
            task.getOutPorts().set(extension.getHexagonalArchitecture().getOutPorts());
            task.getDomainModel().set(extension.getHexagonalArchitecture().getDomainModel());
            task.getAdapters().set(extension.getHexagonalArchitecture().getAdapters());
            task.getApplicationServices().set(extension.getHexagonalArchitecture().getApplicationServices());
            task.getCommonPackages().set(extension.getHexagonalArchitecture().getCommonPackages());

            task.getPresentation().set(extension.getLayeredArchitecture().getPresentation());
            task.getLayeredApplication().set(extension.getLayeredArchitecture().getApplication());
            task.getLayeredDomain().set(extension.getLayeredArchitecture().getDomain());
            task.getInfrastructure().set(extension.getLayeredArchitecture().getInfrastructure());
        });
    }
}