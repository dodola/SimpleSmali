/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dodola.flower.dex;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.util.jcommander.Command;
import org.jf.util.jcommander.ExtendedParameter;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class DexInputCommand extends Command {

    @Parameter(names = { "-a", "--api" },
            description = "The numeric api level of the file being disassembled.")
    @ExtendedParameter(argumentNames = "api") public int apiLevel = 15;

    @Parameter(description = "A dex/apk/oat/odex file. For apk or oat files that contain multiple dex " +
            "files, you can specify the specific entry to use as if the apk/oat file was a directory. " +
            "e.g. \"app.apk/classes2.dex\". For more information, see \"baksmali help input\".")
    @ExtendedParameter(argumentNames = "file") protected List<String> inputList = Lists.newArrayList();

    protected File inputFile;
    protected String inputEntry;
    protected DexBackedDexFile dexFile;

    public DexInputCommand(List<JCommander> commandAncestors) {
        super(commandAncestors);
    }

    protected void loadDexFile(String input) {
        File file = new File(input);

        while (file != null && !file.exists()) {
            file = file.getParentFile();
        }

        if (file == null || !file.exists() || file.isDirectory()) {
            System.err.println("Can't find file: " + input);
            System.exit(1);
        }

        inputFile = file;

        String dexEntry = null;
        if (file.getPath().length() < input.length()) {
            dexEntry = input.substring(file.getPath().length() + 1);
        }

        if (!Strings.isNullOrEmpty(dexEntry)) {
            boolean exactMatch = false;
            if (dexEntry.length() > 2 && dexEntry.charAt(0) == '"' && dexEntry.charAt(dexEntry.length() - 1) == '"') {
                dexEntry = dexEntry.substring(1, dexEntry.length() - 1);
                exactMatch = true;
            }

            inputEntry = dexEntry;

            try {
                dexFile = DexFileFactory.loadDexEntry(file, dexEntry, exactMatch, Opcodes.forApi(apiLevel));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            try {
                dexFile = DexFileFactory.loadDexFile(file, Opcodes.forApi(apiLevel));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}