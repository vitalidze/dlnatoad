package com.vaguehope.dlnatoad.dlnaserver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.BrowseResult;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.Item;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DIDLParser.class, ContentDirectoryService.class })
public class ContentDirectoryServiceTest {

	private static final String DIDL_XML = "didl xml";

	private ContentTree contentTree;
	private DIDLParser didlParser;
	private ContentDirectoryService undertest;

	@Before
	public void before () throws Exception {
		this.contentTree = new ContentTree();
		this.didlParser = mockDidlParser();
		this.undertest = new ContentDirectoryService(this.contentTree);
	}

	@Test
	public void itReturnsAllItemsWhenTheyAreInsideTheRequetsRange () throws Exception {
		List<ContentNode> dirs = givenMockDirs(3);
		List<ContentNode> items = givenMockItems(3);
		addMockItem("item other", dirs.get(1));

		BrowseResult ret = this.undertest.browse(this.contentTree.getRootNode().getId(), BrowseFlag.DIRECT_CHILDREN, null, 0, 10, null);

		assertCorrectResult(ret, 6, 6);
		assertParserMarshaled(dirs, items);
	}

	@Test
	public void itDoesNotMindIfMaxIsMoreThanCount () throws Exception {
		List<ContentNode> dirs = givenMockDirs(3);
		List<ContentNode> items = givenMockItems(3);
		addMockItem("item other", dirs.get(1));

		BrowseResult ret = this.undertest.browse(this.contentTree.getRootNode().getId(), BrowseFlag.DIRECT_CHILDREN, null, 0, 20, null);

		assertCorrectResult(ret, 6, 6);
		assertParserMarshaled(dirs, items);
	}

	@Test
	public void itReturnsFirstFewDirsWhenOnlyDirsInsideRange () throws Exception {
		List<ContentNode> dirs = givenMockDirs(5);

		BrowseResult ret = this.undertest.browse(this.contentTree.getRootNode().getId(), BrowseFlag.DIRECT_CHILDREN, null, 0, 3, null);

		assertCorrectResult(ret, 3, 5);
		assertParserMarshaled(dirs.subList(0, 3), null);
	}

	@Test
	public void itReturnsFirstItemsWhenOnlyItemsInsideRange () throws Exception {
		List<ContentNode> items = givenMockItems(10);

		BrowseResult ret = this.undertest.browse(this.contentTree.getRootNode().getId(), BrowseFlag.DIRECT_CHILDREN, null, 0, 3, null);

		assertCorrectResult(ret, 3, 10);
		assertParserMarshaled(null, items.subList(0, 3));
	}

	@Test
	public void itReturnsDirsThenSomeItemsWhenNotAllItemsFitInsideRange () throws Exception {
		List<ContentNode> dirs = givenMockDirs(5);
		List<ContentNode> items = givenMockItems(10);

		BrowseResult ret = this.undertest.browse(this.contentTree.getRootNode().getId(), BrowseFlag.DIRECT_CHILDREN, null, 0, 10, null);

		assertCorrectResult(ret, 10, 15);
		assertParserMarshaled(dirs, items.subList(0, 5));
	}

	@Test
	public void itReturnsMidRangeDirsWhenOnlyTheyFallInsideRange () throws Exception {
		List<ContentNode> dirs = givenMockDirs(10);
		givenMockItems(10);

		BrowseResult ret = this.undertest.browse(this.contentTree.getRootNode().getId(), BrowseFlag.DIRECT_CHILDREN, null, 3, 3, null);

		assertCorrectResult(ret, 3, 20);
		assertParserMarshaled(dirs.subList(3, 6), null);
	}

	@Test
	public void itReturnsLastFewDirsThenSomeItems () throws Exception {
		List<ContentNode> dirs = givenMockDirs(10);
		List<ContentNode> items = givenMockItems(10);

		BrowseResult ret = this.undertest.browse(this.contentTree.getRootNode().getId(), BrowseFlag.DIRECT_CHILDREN, null, 5, 10, null);

		assertCorrectResult(ret, 10, 20);
		assertParserMarshaled(dirs.subList(5, 10), items.subList(0, 5));
	}

	@Test
	public void itReturnsMidRangeItems () throws Exception {
		givenMockDirs(10);
		List<ContentNode> items = givenMockItems(10);

		BrowseResult ret = this.undertest.browse(this.contentTree.getRootNode().getId(), BrowseFlag.DIRECT_CHILDREN, null, 14, 2, null);

		assertCorrectResult(ret, 2, 20);
		assertParserMarshaled(null, items.subList(4, 6));
	}

	@Test
	public void itReturnsLastFewItems () throws Exception {
		givenMockDirs(10);
		List<ContentNode> items = givenMockItems(10);

		BrowseResult ret = this.undertest.browse(this.contentTree.getRootNode().getId(), BrowseFlag.DIRECT_CHILDREN, null, 15, 5, null);

		assertCorrectResult(ret, 5, 20);
		assertParserMarshaled(null, items.subList(5, 10));
	}

	@Test
	public void itReturnsItemsFromSubDir () throws Exception {
		List<ContentNode> rootDirs = givenMockDirs(5);
		List<ContentNode> items = givenMockItems(15, rootDirs.get(0));

		BrowseResult ret = this.undertest.browse(rootDirs.get(0).getId(), BrowseFlag.DIRECT_CHILDREN, null, 0, 10, null);

		assertCorrectResult(ret, 10, 15);
		assertParserMarshaled(null, items.subList(0, 10));
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private List<ContentNode> givenMockDirs (int n) {
		List<ContentNode> ret = new ArrayList<ContentNode>();
		for (int i = 0; i < n; i++) {
			ret.add(addMockDir("dir " + i));
		}
		return ret;
	}

	private List<ContentNode> givenMockItems (int n) {
		return givenMockItems(n, this.contentTree.getRootNode());
	}

	private List<ContentNode> givenMockItems (int n, ContentNode parent) {
		List<ContentNode> ret = new ArrayList<ContentNode>();
		for (int i = 0; i < n; i++) {
			ret.add(addMockItem("item " + i, parent));
		}
		return ret;
	}

	private ContentNode addMockDir (String id) {
		return addMockDir(id, this.contentTree.getRootNode());
	}

	private ContentNode addMockDir (String id, ContentNode parent) {
		Container container = new Container();
		container.setId(id);
		container.setChildCount(Integer.valueOf(0));

		ContentNode node = new ContentNode(id, container);
		this.contentTree.addNode(node);
		parent.getContainer().addContainer(node.getContainer());
		parent.getContainer().setChildCount(parent.getContainer().getChildCount() + 1);
		return node;
	}

	private ContentNode addMockItem (String id, ContentNode parent) {
		Item item = mock(Item.class);
		when(item.toString()).thenReturn("item " + id);
		ContentNode node = new ContentNode(id, item, mock(File.class));
		this.contentTree.addNode(node);
		parent.getContainer().addItem(node.getItem());
		parent.getContainer().setChildCount(parent.getContainer().getChildCount() + 1);
		return node;
	}

	private static DIDLParser mockDidlParser () throws Exception {
		DIDLParser didlParser = mock(DIDLParser.class);
		PowerMockito.whenNew(DIDLParser.class).withNoArguments().thenReturn(didlParser);
		when(didlParser.generate(isA(DIDLContent.class))).thenReturn(DIDL_XML);
		return didlParser;
	}

	private static void assertCorrectResult (BrowseResult ret, long count, long totalMatches) {
		assertEquals(count, ret.getCountLong());
		assertEquals(totalMatches, ret.getTotalMatchesLong());
		assertEquals(DIDL_XML, ret.getResult());
	}

	private void assertParserMarshaled (List<ContentNode> dirs, List<ContentNode> items) throws Exception {
		ArgumentCaptor<DIDLContent> cap = ArgumentCaptor.forClass(DIDLContent.class);
		verify(this.didlParser).generate(cap.capture());
		assertEquals(listOfContainers(dirs), cap.getValue().getContainers());
		assertEquals(listOfItems(items), cap.getValue().getItems());
	}

	private static List<Container> listOfContainers (List<ContentNode> nodes) {
		List<Container> l = new ArrayList<Container>();
		if (nodes != null) {
			for (ContentNode cn : nodes) {
				l.add(cn.getContainer());
			}
		}
		return l;
	}

	private static List<Item> listOfItems (List<ContentNode> nodes) {
		List<Item> l = new ArrayList<Item>();
		if (nodes != null) {
			for (ContentNode cn : nodes) {
				l.add(cn.getItem());
			}
		}
		return l;
	}

}
