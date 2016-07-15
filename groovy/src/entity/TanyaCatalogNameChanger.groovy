package entity
/**
 * Created with IntelliJ IDEA.
 * Used this script to change Mac-Automator JPEGs to prefix the name of the fly line (the directory name)
 * User: saffordt
 * Date: 3/5/13
 * Time: 10:00 AM
 */
File imageDir = new File("/Users/saffordt/Desktop/NewDocs");
File[] dirs = imageDir.listFiles(new FileFilter() {
    @Override
    boolean accept(File file) {
        return file.isDirectory();
    }
});
for (File tmpDir : dirs) {
    File[] jpegs = tmpDir.listFiles(new FilenameFilter() {
        @Override
        boolean accept(File file, String s) {
            return s.toLowerCase().endsWith(".jpeg");
        }
    })
    for (int i = 0; i < jpegs.length; i++) {
        File tmpjpeg = jpegs[i];
        String newname = tmpDir.getName()+tmpjpeg.getName().substring(tmpjpeg.getName().lastIndexOf("-"));
        boolean success = tmpjpeg.renameTo(tmpDir.getAbsolutePath()+File.separator+newname);
        if (!success) {
            println "Name change failed for " + tmpjpeg.getName();
        }
    }
}
