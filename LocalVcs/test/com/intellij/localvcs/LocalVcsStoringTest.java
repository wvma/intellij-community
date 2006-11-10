package com.intellij.localvcs;

import org.junit.Before;
import org.junit.Test;

public class LocalVcsStoringTest extends TempDirTestCase {
  private LocalVcs vcs;

  @Before
  public void setUp() {
    vcs = createVcs();
  }

  private LocalVcs createVcs() {
    return new LocalVcs(new Storage(tempDir));
  }

  @Test
  public void testCleanStorage() {
    Storage s = new Storage(tempDir);

    ChangeList changeList = s.loadChangeList();
    RootEntry entry = s.loadRootEntry();
    Integer counter = s.loadCounter();

    assertTrue(changeList.getChangeSets().isEmpty());
    assertTrue(entry.getChildren().isEmpty());
    assertEquals(0, counter);
  }

  @Test
  public void testStoringEntries() {
    vcs.createFile(p("file"), "content");
    vcs.apply();

    vcs.store();
    LocalVcs result = createVcs();

    assertTrue(result.hasEntry(p("file")));
  }

  @Test
  public void testStoringChangeList() {
    vcs.createFile(p("file"), "content");
    vcs.apply();
    vcs.changeFileContent(p("file"), "new content");
    vcs.apply();

    vcs.store();
    LocalVcs result = createVcs();

    assertEquals("new content", result.getEntry(p("file")).getContent());

    result.revert();
    assertEquals("content", result.getEntry(p("file")).getContent());

    result.revert();
    assertFalse(result.hasEntry(p("file")));
  }

  @Test
  public void testStoringObjectsCounter() {
    vcs.createFile(p("file1"), "content1");
    vcs.createFile(p("file2"), "content2");
    vcs.apply();

    vcs.store();
    LocalVcs result = createVcs();

    result.createFile(p("file3"), "content3");
    result.apply();

    Integer id2 = result.getEntry(p("file2")).getObjectId();
    Integer id3 = result.getEntry(p("file3")).getObjectId();

    assertTrue(id2 < id3);
  }

  @Test
  public void testDoesNotStoreUnappliedChanges() {
    vcs.createFile(p("file"), "content");
    vcs.store();

    vcs.store();
    LocalVcs result = createVcs();
    assertTrue(result.isClean());
  }
}
