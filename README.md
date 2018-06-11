# The Argument Reasoning Comprehension Task: Identification and Reconstruction of Implicit Warrants 

Source code, data, and supplementary materials for our NAACL 2018 paper and for SemEval 2018 shared task.
Use the following citation if you use any of the code or the data set:

```
@InProceedings{Habernal.et.al.2018.NAACL.ARCT,
  title     = {The Argument Reasoning Comprehension Task: Identification
               and Reconstruction of Implicit Warrants},
  author    = {Habernal, Ivan and Wachsmuth, Henning and
               Gurevych, Iryna and Stein, Benno},
  publisher = {Association for Computational Linguistics},
  booktitle = {Proceedings of the 2018 Conference of the North American Chapter
               of the Association for Computational Linguistics:
               Human Language Technologies, Volume 1 (Long Papers)},
  pages     = {1930--1940},
  month     = jun,
  year      = {2018},
  address   = {New Orleans, Louisiana},
  url       = {http://aclweb.org/anthology/N18-1175}
```

> **Abstract** Reasoning is a crucial part of natural language argumentation. To comprehend an argument, one must analyze its warrant, which explains why its claim follows from its premises. As arguments are highly contextualized, warrants are usually presupposed and left implicit. Thus, the comprehension does not only require language understanding and logic skills, but also depends on common sense. In this paper we develop a methodology for reconstructing warrants systematically. We operationalize it in a scalable crowdsourcing process, resulting in a freely licensed dataset with warrants for 2k authentic arguments from news comments. On this basis, we present a new challenging task, the argument reasoning comprehension task. Given an argument with a claim and a premise, the goal is to choose the correct implicit warrant from two options. Both warrants are plausible and lexically close, but lead to contradicting claims. A solution to this task will define a substantial step towards automatic warrant reconstruction. However, experiments with several neural attention and language models reveal that current approaches do not suffice.

* Contact person: Ivan Habernal, habernal@ukp.informatik.tu-darmstadt.de
  * UKP Lab: http://www.ukp.tu-darmstadt.de/
  * TU Darmstadt: http://www.tu-darmstadt.de/

For license information, see LICENSE and NOTICE.txt. This repository contains experimental software and is published for the sole purpose of giving additional background details on the respective publication.

## SemEval2018 Data

Data for SemEval are in a simple plaintext tab-separated format. The first line is a comment (column headers) and each line is then a single instance in the following form:


| id | warrant0 | warrant1 | correctLabelW0orW1 | reason | claim | debateTitle | debateInfo |
| --- | --- | --- | --- | ---| --- | --- | --- |
| 9975147_345_A34QZDSTKZ3JO9 | scholarships would give women a chance to study | scholarships would take women from the home | 0 | Miss America gives honors and education scholarships. | Miss America is good for women | There She Is, Miss America	 | In 1968, feminists gathered in Atlantic City to protest the Miss America pageant, calling it racist and sexist. Is this beauty contest bad for women? |

where `warrant0` and `warrant1` are *warrant* and *alternative warrant*. If `correctLabelW0orW1` is `0`, then `warrant0` is correct (thus the *warrant*) and `warrant1` is incorrect (thus the *alternative warrant*) for this particular argument. Similarly, if `correctLabelW0orW1` is `1`, then `warrant0` is the *alternative warrant* and thus incorrect, and `warrant1` is the *warrant* and thus correct.

**How to understand the task?**

*Reason.* And since *warrant,* *claim.*

From the example above, we can construct this argument:

*Miss America gives honors and education scholarships.* And since *scholarships would give women a chance to study,* *Miss America is good for women*

is the correct explanation, as `warrant0` is labeled as correct in the data. Using `warrant1` would lead to an inconsistent (or simply wrong) argument:

*Miss America gives honors and education scholarships.* And since *scholarships would take women from the home,* *Miss America is good for women*




There are the following files:

```
mturk/annotation-task/data/exported-SemEval2018-train-dev-test/dev-full.txt
mturk/annotation-task/data/exported-SemEval2018-train-dev-test/dev-only-data.txt
mturk/annotation-task/data/exported-SemEval2018-train-dev-test/dev-only-labels.txt
mturk/annotation-task/data/exported-SemEval2018-train-dev-test/train-full.txt
mturk/annotation-task/data/exported-SemEval2018-train-dev-test/train-w-swap-full.txt
```

where "full" means the file contains both data and gold labels as shown above, "only-labels" are just IDs and labels, and "only-data" are data without gold labels. The "only-data" variant will be provided for the test data.

"w-swap-full" is a double-sized training data; each instance is shown twice - on the first line the ordering of `warrant0` and `warrant1` is left as in the original data, on the second line the ordering is swapped.


## Sub-modules

### Module `roomfordebate`

`roomfordebate` contains utilities for crawling articles and debates from the Room for Debate (RFD) site of NYT

  * Used for initial fetching the raw data. It requires Google Chrome/Chromium to be installed as well as `Xvfb` for running Selenium window-less. This entire step might break in the future due to newer versions of Chrome or NYT page changes.
  * The `src/main/resources` directory contains a list of all URLs from RFD between 2010 and 2017 (split into several batches) as well as a list of topics in a tsv file. The topics were manually pre-selected and enriched with two explicit stances each (see `rfd-controversies/rfd-manual-cleaning-controversies.*`).
  * Workflow overview:
    * Step 0a: Pre-crawl the entire NYT website using a crawler (such as Apache Nutch) that outputs `warc.gz` files
    * Step 0b: `URLsFromWarcExtractor` extracts all URLs from the `roomfordebate` sub-section of NYT
    * Step 0c: Manual inspection of URLs, filtering controversial topics, assigning explicit stances
    * Step 1: `MainCrawler` downloads all URLs in HTML files
    * Step 2: `DebateHTMLParser` extracts the content from HTML (debates and comments) and produces a XML file
    TODO where is the file? Attach it to the submission?
    
#### Technical details

* Requirements
    * `Chromium` browser
    * Chrome driver for Selenium (included in this package)
    * `xvfb` (virtual X11 server) for redirecting the visual output of Chromium
* For fetching debates, run `xvfb` on the same computer as the crawler (can be desktop or server)
```
$ /usr/bin/Xvfb :20 -screen 0 1280x4096x24 -ac +extension GLX +render -noreset
```

### 3-rd party licenses
    
* https://sites.google.com/a/chromium.org/chromedriver/home
    * "All code is currently in the open source Chromium project. This project is developed by members of the Chromium and WebDriver teams."

### Room for debate - legal information

* Hosted by The New York Times: http://www.nytimes.com/roomfordebate/
* Contains articles and comments
    * Copyright holders for articles are their authors (not NYT), copyright holders for the comments are their authors (not NYT)
* Inquiry sent to PARS International Corp. 253 West 35th Street, 7th Floor, New York, NY 10001 with the following answer:
    * > [...] Please be advised that most of the content copyrights from the Room For Debate Section are not owned by The New York Times. You will need to contact each individual author per article piece individually, for any article that appears in that section. [...] 
Maria Barrera-Kozluk, Content Permission Specialist, 212-221-9595, ext. 407, maria.barrera@parsintl.com



### Module `model`
  
* `model` contains simple plain-old-java-object containers for articles and comments

### Module `segmenter`

* `segmenter` contains only a single class that splits the given sentences into Elementary Discourse Units (EDUs). It is in its own module due to conflicting classes in the implementation of the EDU segmenter and other DKPro classes.

### Module `corpus-creation`


#### Classes `Step0*`

These classes provide filtering of the RFD comments and their linguistic preprocessing (sentence and EDU splitting). Segmentation to EDU must be done in three steps, relying on the other module `segmenter`.

The output is a single big file `mturk/annotation-task/data/arguments-with-full-segmentation-rfd.xml.gz`.



## Other files

`mturk/annotation-task/archive-hits-pilots-etc` is an archive of pilot HIT tasks, intermediate tasks, and also many unsuccessful attempts that eventually did not work.



## Data


### Available data


All the following files are located in the `mturk/annotation-task/data` folder.

#### Stance detection

`22-stance-batch-0001-5000-all.xml.gz` gold-annotated data after the first step (Stance annotation). The statistics are

```
Value                                          Freq.       Pct.
Annotated with one of the two stances          2884        58%
The same as above but also sarcastic argument   481        10%
Takes both stances but remains neutral          285         6%
The same as above but also sarcastic             18         0%
No stance (independent of sarcasm)             1083        22%
Discarded by MACE                               249         5%
 ```

`22-stance-batch-0001-5000-only-with-clear-stances.xml.gz` then contains 2884 non-sarcastic arguments that take one of the two stances in the XML format.

We also provide 3365 stance-taking arguments (including sarcastic ones) exported in a tab-separated plain text format in `mturk/annotation-task/data/exported-3365-stance-taking-arguments.tsv`.

For those interested in a 3-way stance detection (stance1, stance2, no-stance), we also exported 4448 comments which is a superset of the 3365 stance-taking arguments described above. This file contains additional 1083 posts annotated as having no stance (they might or might not be sarcastic), see `mturk/annotation-task/data/exported-4448-comments-incl-no-stance.tsv` 

#### Reason spans and reason gist

`32-reasons-batch-0001-5000-2026args-gold.xml.gz` is the output of the second step (Reason span annotations) and contains 2026 arguments annotated with gold-labeled reason spans.
Internally, annotations in the XML file are embedded in UIMA format which is serialized into Base64 encoding (maybe not the 'cleanest' solution but working fine). For working with the annotations, DKPro framework is used internally. Nevertheless, the content can be easily accessed by calling `getJCas()` and `setJCas()` in `StandaloneArgument` (see for example `Step2dGoldReasonStatistics`).

Reason spans (or premise spans, use these two term interchangeably) are annotated using `BIO` tagging which means that each EDU (Elementary Discourse Unit) is labeled as `Premise-B` (beginning of the span), `Premise-I` (inside the span), and `O` (not in a reason). The gold label for each EDU is then computed by MACE without taking the context EDUs into consideration. Gold labels of some EDUs cannot be estimated reliably (have high entropy, see the original MACE paper) and are thus ignored, depending on the threshold value configured in MACE. If an argument contains at least one EDU for which the gold label cannot be estimated, this argument is discarded completely from the gold collection in order to keep high-quality annotations. Furthermore, after manual examination we discarded a couple of arguments labeled with more than six reasons.

From the 2882 input arguments (by mistake, we annotated two arguments fewer than 2884 as stays in the paper), 856 were discarded using this process resulting into 2026 annotated with reason spans.
 
Statistics of the data are computed in `Step2dGoldReasonStatistics` which shows a distribution of number of reasons spans per argument:

```
Arguments: 2026

Reasons per argument:
Value   Freq.    Pct. 
0         44      2%
1        372     18%
2        665     33%
3        539     27%
4        264     13%
5        108      5%
6         34      2%

Mean: 2.527 (std-dev 1.237)
Argument reason spans total: 5119
```

Notice that there are 44 argument in which no explicit reasons supporting the stance were identified.



`42-reasons-gist-batch-0001-5000-4294reasons.xml.gz` is the output of writing gist of each reason in arguments. Each premise in the argument is annotated with the `gist` property (using `ArgumentUnitUtils.setProperty()` method). Some premises were identified as not being an actual reason, there are labeled with a property `not-a-reason`. In total, 1927 arguments with 4294 gists are annotated.

```
2026 arguments were originally to be annotated
Original reason statistics:
IntSummaryStatistics{count=2026, sum=5119, min=0, average=2,526654, max=6}
1927 arguments are in the output dataset
Reason with gist statistics:
LongSummaryStatistics{count=1927, sum=4294, min=1, average=2,228334, max=6}
```

Thus for an argument mining system that detects argument components (premises, in this case), there are 1,927 arguments (documents) available with 4,294 premises in total. Each contains 2,23 premise spans on average along with a summarized gist of each premise. These arguments are exported in the UIMA XMI format compatible with [DKPro-Argumentation](https://github.com/dkpro/dkpro-argumentation) together with a CSV file with all relevant meta-data. The files are in `exported-1927-arguments-with-gold-reasons-xmi.tar.bz2` and were produced by `Step3dExportGistToXMIFiles`. These can be easily converted for example into BIO-annotations to perform argument component identification as sequence labeling, see the [example](https://github.com/dkpro/dkpro-argumentation/blob/master/de.tudarmstadt.ukp.dkpro.argumentation.examples/src/main/java/de/tudarmstadt/ukp/dkpro/argumentation/tutorial/ArgumentationCorpusBIOTokenExporter.java) in DKPro-Argumentation. We exported the data in CoNLL format, see below.

#### Reason disambiguation

`61-reason-disambiguation-batch-0001-5000-4235reasons.xml.gz` is the output of reason disambiguation (step 4). Each premise is annotated with one of the following categories:

```
Saving 1919 arguments with 4235 disambiguated premises
Value               Freq.  Pct. 
BOTH_POSSIBLE        996    24%
OPPOSITE             679    16%
ORIGINAL            1955    46%
REPHRASED_ORIGINAL   605    14%
```

Only those premises with "ORIGINAL" stance are used for the later experiments. It means, these premises heavily presuppose the overall stance so it's possible to draw it directly from it.

### Argument Component Identification in CoNLL format (BIO)

Reason (premises) spans are also directly available in plain-text files using a CoNLL tab-separated format. The following example is taken from `2264611.txt`:

```
Many    O
TFA    O
folks    O
are    O
feeling    O
defensive    O
these    O
days    O
...
.    O
The    Premise-B
program    Premise-I
promises    Premise-I
more    Premise-I
than    Premise-I
it    Premise-I
can    Premise-I
deliver    Premise-I
.    Premise-I
...
the    Premise-I
silver    Premise-I
bullet    Premise-I
for    Premise-I
all    Premise-I
education's    Premise-I
problems    Premise-I
.    Premise-I
There    Premise-B
is    Premise-I
nothing    Premise-I
wrong    Premise-I
with    Premise-I
leaving    Premise-I
education    Premise-I
...
```

Each line contains a single token, a tab-character, and the corresponding BIO label.


Folder `mturk/annotation-task/data/exported-1927-arguments-with-gold-reasons-conll` contains 1,927 exported arguments with annotated premises in this format along with `metatada.csv` containing all relevant meta-data information for each file. The files were exported using `de.tudarmstadt.ukp.experiments.exports.Step2eExportPremisesBIOEncoding`. 

 
### Crowd labeling sequences - data for agreement experiments

`exported-reason-spans-pilot-group1.csv` and `exported-reason-spans-pilot-group2.csv` are crowd worker annotations from the pilot for reason span annotation; each item was labeled by 18 workers. The items correspond to EDU (Elementary Discourse Units) and the labels are BIO tags. Each item has thus 18 assignments.
 
The assignments are split to two groups (9+9) given by the submission time, so two files for two groups of workers are created; see the paper for details about this setup.
  
The output format is a simple self-explaining CSV file, in the following format

```
" ",A10S5LDYYYB496,A1FGKIKJYSL1MI,A1LLT1N2U68K50,A1OYYWJ2B7OQTD, ....
12014246_00,-,O,O,O,-,-,-,O,-,O,-,-,-,O,Premise-B,-,O, ...
12014246_01,-,Premise-B,Premise-B,Premise-B,-,-,-,O,-,O,-, ...
12014246_02,-,Premise-I,Premise-I,Premise-I,-,-,-,O,-,O,-,-, ...
12014246_03,-,Premise-I,Premise-I,Premise-B,-,-,-,O,-,O,-,-,-, ...
...
```

where the columns are worker IDs, rows are composed of "argumentId_EDUid" (so they are ordered sequences of EDUs as in the original texts), and the values are "Premise-B", "Premise-I", "O" or "-" if no value is available for this worker (which means that the worker did not work on this task).

### Abstractive argument summarization

Folder `exported-1927-summarized-arguments` contains a single tab-separated file with 1,927 arguments and their abstractive summaries. The summaries were created by compiling gists of the reasons thus convey the most important information from the argument. All relevant metadata are stored in `metadata.csv`.


## Running Python experiments

### Prerequisites

Requirements: Python 3.X, `virtualenv` package

Install Keras, NLTK, scikit and other libraries in the virtual environment.

This experiments use Theano as Kereas' backend and haven't been tested with TensorFlow.

```
$ cd experiments/src/main/python
$ virtualenv env-python3-keras110 --python=python3
$ source env-python3-keras110/bin/activate
$ pip3 install Keras==1.1.0 nltk==3.1 scikit-learn==0.17.1 pydot-ng==1.0.0 pydot==1.2.2
```

Note: if `pip3` fails due to a bad interpreter, call it by using `$ python env-python3-keras110/bin/pip` instead.

Install Gensim for converting word2vec binary to txt (optional)

```
$ pip3 install gensim==0.13.2
```

### Setting up a HPC cluster (caveats)

* Make sure your `~/.local/` is empty (except for some irrelevant stuff)
* `virtualenv` does not work on HPC, so `--user` must be used in PIP

```
$ module load python intel graphviz
$ pip3 install --user Keras==1.1.0 nltk==3.1 scikit-learn==0.17.1
```

### Running experiments

```
$ cd experiments/src/main/python
$ KERAS_BACKEND=theano python main.py 
```
