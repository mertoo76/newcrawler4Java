# newcrawler4java

Crawler4j project with depth search and best-first search algorithms The first crawling strategy is depth-first search. In depth-first search, the frontier acts like a last-in first-out stack. The elements are added to the stack one at a time. The one selected and taken off the frontier at any time is the last element that was added.

The second crawling strategy is the best-first crawler. In this strategy pages are visited in the order specified by the priority values in the frontier. The priority is specified based on an estimate of the value of the linked page. The estimate can be based on topological properties (e.g. the in-degree of the target page) or content properties (e.g. the similarity between a query keyword and the source page).

 ## Import
You Have To Build lib files again. Right click project folder Build path -> Configure build path-> libraries and add external jar in the zip files and remove old jars. 

## Usage

Run at BasicCrawController.1 for DFS, 2 for BFS. For the first one it is simple depth first search. For the second one crawler crawl 'bios' page firstly then crawl other page so we will give priority if web page had 'bios'.
