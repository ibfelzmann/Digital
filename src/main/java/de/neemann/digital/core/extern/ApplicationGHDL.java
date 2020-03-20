/*
 * Copyright (c) 2018 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.core.extern;

import de.neemann.digital.core.element.Keys;
import de.neemann.digital.core.extern.handler.ProcessInterface;
import de.neemann.digital.core.extern.handler.StdIOInterface;
import de.neemann.digital.gui.Settings;
import de.neemann.digital.lang.Lang;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Abstraction of the ghdl Application.
 * See https://github.com/ghdl/ghdl
 */
public class ApplicationGHDL extends ApplicationVHDLStdIO {

    @Override
    public ProcessInterface start(String label, String code, PortDefinition inputs, PortDefinition outputs, String appOptions) throws IOException {
        File file = null;
        try {
            String ghdl = getGhdlPath().getPath();

            file = createVHDLFile(label, code, inputs, outputs);

            ArrayList<String> cmd = new ArrayList<>();
            ArrayList<String> optionsList = new ArrayList<>();
            for (String o: Arrays.asList(appOptions.split("(?<!\\\\)\\s+"))) {
              optionsList.add(o.replaceAll("\\\\(\\s+)", "$1"));
            }

            cmd.add(ghdl);
            cmd.add("-a");
            cmd.addAll(optionsList);
            cmd.add(file.getName());
            ProcessStarter.start(file.getParentFile(), cmd.toArray(new String[cmd.size()]));
            cmd.clear();

            cmd.add(ghdl);
            cmd.add("-e");
            cmd.addAll(optionsList);
            cmd.add("stdIOInterface");
            ProcessStarter.start(file.getParentFile(), cmd.toArray(new String[cmd.size()]));
            cmd.clear();

            cmd.add(ghdl);
            cmd.add("-r");
            cmd.addAll(optionsList);
            cmd.add("stdIOInterface");
            cmd.add("--unbuffered");
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true).directory(file.getParentFile());

            return new GHDLProcessInterface(pb.start(), file.getParentFile());
        } catch (IOException e) {
            if (file != null)
                ProcessStarter.removeFolder(file.getParentFile());
            if (ghdlNotFound(e))
                throw new IOException(Lang.get("err_ghdlNotInstalled"));
            else
                throw e;
        }
    }

    private boolean ghdlNotFound(Throwable e) {
        while (e != null) {
            if (e instanceof ProcessStarter.CouldNotStartProcessException)
                return true;
            e = e.getCause();
        }
        return false;
    }

    @Override
    public boolean checkSupported() {
        return true;
    }

    @Override
    public String checkCode(String label, String code, PortDefinition inputs, PortDefinition outputs, String appOptions) throws IOException {
        File file = null;
        try {
            String ghdl = getGhdlPath().getPath();

            file = createVHDLFile(label, code, inputs, outputs);
            ArrayList<String> cmd = new ArrayList<>();
            ArrayList<String> optionsList = new ArrayList<>();
            for (String o: Arrays.asList(appOptions.split("(?<!\\\\)\\s+"))) {
              optionsList.add(o.replaceAll("\\\\(\\s+)", "$1"));
            }

            cmd.add(ghdl);
            cmd.add("-a");
            cmd.addAll(optionsList);
            cmd.add(file.getName());
            String m1 = ProcessStarter.start(file.getParentFile(), cmd.toArray(new String[cmd.size()]));
            cmd.clear();

            cmd.add(ghdl);
            cmd.add("-e");
            cmd.addAll(optionsList);
            cmd.add("stdIOInterface");
            String m2 = ProcessStarter.start(file.getParentFile(), cmd.toArray(new String[cmd.size()]));
            return ProcessStarter.joinStrings(m1, m2);
        } catch (IOException e) {
            if (ghdlNotFound(e))
                throw new IOException(Lang.get("err_ghdlNotInstalled"));
            else
                throw e;
        } finally {
            if (file != null)
                ProcessStarter.removeFolder(file.getParentFile());
        }
    }

    private static File getGhdlPath() {
        return Settings.getInstance().get(Keys.SETTINGS_GHDL_PATH);
    }

    private static final class GHDLProcessInterface extends StdIOInterface {
        private final File folder;

        private GHDLProcessInterface(Process process, File folder) {
            super(process);
            this.folder = folder;
        }

        @Override
        public String getConsoleOutNoWarn(LinkedList<String> consoleOut) {
            StringBuilder sb = new StringBuilder();
            for (String s : consoleOut) {
                if (!s.contains("(assertion warning)"))
                    sb.append(s).append("\n");
            }
            return sb.toString();
        }

        @Override
        public void close() throws IOException {
            super.close();
            ProcessStarter.removeFolder(folder);
        }
    }
}
