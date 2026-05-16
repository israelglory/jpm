package com.jpm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI entrypoint. Small delegator that routes commands and prints friendly messages.
 */
public final class Main {

    private static final String USAGE = String.join(System.lineSeparator(),
            "JPM - Java Package Manager",
            "",
            "Usage:",
            "  jpm add <alias>",
            "",
            "Supported aliases (example):",
            "  postgres",
            "  lombok",
            "  web");

    private static final String VERSION = "1.0.3";

    private final DependencyResolver dependencyResolver;
    private final PomEditor pomEditor;

    public Main() {
        this.dependencyResolver = new DependencyResolver(new MavenCentralClient());
        this.pomEditor = new PomEditor();
    }

    public static void main(String[] args) {
        int exitCode = new Main().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    int run(String[] args) {
        if (args == null || args.length == 0) {
            printUsage();
            return 1;
        }

        String command = args[0].trim().toLowerCase();
        if (command.isEmpty()) {
            printUsage();
            return 1;
        }

        return switch (command) {
            case "add" -> handleAdd(args);
            case "help", "--help", "-h" -> {
                printUsage();
                yield 0;
            }
            case "version", "--version", "-v" -> {
                System.out.println("JPM version " + VERSION);
                yield 0;
            }
            default -> {
                System.out.println("✘ Invalid command");
                printUsage();
                yield 1;
            }
        };
    }

    private int handleAdd(String[] args) {
        if (args.length != 2) {
            System.out.println("✘ Invalid command");
            printUsage();
            return 1;
        }

        Path pomPath = Path.of("pom.xml");
        if (!Files.exists(pomPath)) {
            System.out.println("✘ No pom.xml found");
            return 1;
        }

        String alias = args[1].trim();
        if (alias.isEmpty()) {
            System.out.println("✘ Invalid command");
            printUsage();
            return 1;
        }

        try {
            Dependency dependency = dependencyResolver.resolve(alias);
            PomEditor.UpdateStatus updateStatus = pomEditor.addDependency(pomPath, dependency);

            if (updateStatus == PomEditor.UpdateStatus.ALREADY_EXISTS) {
                System.out.println("✔ Dependency already exists");
            } else {
                System.out.println("✔ Dependency added successfully");
            }
            return 0;
        } catch (DependencyResolver.UnknownDependencyAliasException ex) {
            System.out.println("✘ Dependency not found");
            return 1;
        } catch (DependencyResolver.DependencyVersionNotFoundException ex) {
            System.out.println("✘ Dependency not found");
            return 1;
        } catch (IOException ex) {
            System.out.println("✘ Failed to fetch dependency version");
            return 1;
        } catch (PomEditor.PomEditException ex) {
            if ("No pom.xml found".equals(ex.getMessage())) {
                System.out.println("✘ No pom.xml found");
            } else if ("Invalid pom.xml".equals(ex.getMessage())) {
                System.out.println("✘ Invalid pom.xml");
            } else {
                System.out.println("✘ Failed to update pom.xml");
            }
            return 1;
        }
    }

    private void printUsage() {
        System.out.println(USAGE);
    }
}

