package com.vaguehope.dlnatoad.util;

import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

/**
 * Provides canonical temporary folder since it may be based on symlink on Mac OS X.
 * Used to fix tests.
 */
public class TmpFolder extends TemporaryFolder {
    @Override
    public File getRoot() {
        File root = super.getRoot();
        try {
            if (!root.getAbsolutePath().equals(root.getCanonicalPath())) {
                return root.getCanonicalFile();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return root;
    }
}
