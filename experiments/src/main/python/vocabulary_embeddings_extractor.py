import bz2
import os
import unicodedata

import numpy as np
import six.moves.cPickle as cPickle
from nltk.tokenize.casual import TweetTokenizer


def tokenize(s):
    """
    Tokenization of the given text using TweetTokenizer delivered along with NLTK
    :param s: text
    :return: list of tokens
    """
    sentence_splitter = TweetTokenizer()
    tokens = sentence_splitter.tokenize(s)
    result = []
    for word in tokens:
        # the last "decode" function is because of Python3
        # http://stackoverflow.com/questions/2592764/what-does-a-b-prefix-before-a-python-string-mean
        w = unicodedata.normalize('NFKD', word).encode('ascii', 'ignore').decode('utf-8').strip()
        # and add only if not empty (it happened in some data that there were empty tokens...)
        if w:
            result.append(w)

    return result


def load_vocabulary_frequencies(files):
    """
    Loads vocabulary with frequencies from given documents sorted by frequency
    :param files: list of input files in the format label TAB title TAB text; they can be csv or gzipped csv
    """
    assert isinstance(files, list)

    word_frequencies = dict()

    for single_file in files:
        if os.path.isfile(single_file):
            # the files can be either plain text csv of gzipped csv
            _, ext = os.path.splitext(single_file)
            f = None
            if '.csv' == ext or '.tsv' == ext:
                f = open(single_file)
            if '.gz' == ext:
                import gzip
                f = gzip.open(single_file, 'rb')

            for line in f:
                # we have to again convert from bytes to string in Python3... ugly... :/
                l = line
                if isinstance(line, bytes):
                    l = line.decode('utf-8').strip()

                split = l.split("\t")
                # concat all texts (split[3] is label)
                texts = split[1] + " " + split[2] + " " + split[4] + " " + split[5] + " " + split[6] + " " + split[7]
                # tokenize
                tokens = tokenize(texts)

                # update frequency
                for word in tokens:
                    # add empty entry
                    word_frequencies[word] = word_frequencies.get(word, 0) + 1

    return word_frequencies


def extract_word_and_vector_from_glove_file_line(line):
    """
    Given a textual line from a Glove file, it returns its head (word) and the corresponding
    numpy vector
    :param line: single line from the original Glove file
    :return: a tuple (word, [numpy vector])
    """
    partition = line.partition(' ')
    return partition[0], np.fromstring(partition[2], sep=' ')


def extract_word_and_vector_from_word2vec_file_line(line):
    """
    Given a textual line from a word2vec file in TXT format, it returns the head (word) and
    the corresponding numpy vector
    :param line:  single line
    :return: a tuple (word, numpy vector)
    """

    # "decode" function is because of Python3
    # http://stackoverflow.com/questions/2592764/what-does-a-b-prefix-before-a-python-string-mean
    split = line.decode('utf-8').split()

    # ignore the first line
    if len(split) == 2:
        return None, None

    # word
    head = split[0]
    # the rest is the embeddings vector, convert to numpy float array
    vector = np.array(split[1:]).astype(np.float)

    return head, vector


def extract_embeddings_vectors_for_given_words(embeddings_file_name, file_type, smaller_vocabulary):
    """
    Given a limited vocabulary (param 'smaller vocabulary'), it returns Glove vectors for these
    words from a large Glove word vector file (usually several GB large).
    If the word is missing in Glove, the key is omitted from the output dictionary.
    :param embeddings_file_name: original Glove or word2vec TXT file (see convert_word2vec_bin_to_txt.py for
    preparing word2vec in text format)
    :param file_type: either 'glove' or 'word2vec'
    :param smaller_vocabulary: words for which we want to retrieve Glove word vectors
    :return: dictionary {word, np.array vector}
    """
    word_map = dict()

    # param checking
    if file_type not in ['glove', 'word2vec']:
        raise Exception('Unknown embeddings file type:', file_type, 'Must be "glove" or "word2vec"')

    # word2vec txt file can be gzipped
    f = None
    if embeddings_file_name.endswith('gz'):
        import gzip
        f = gzip.open(embeddings_file_name, 'rb')
    else:
        f = open(embeddings_file_name, 'r')

    for line in f:
        head, vec = (None, None)
        if file_type == 'glove':
            head, vec = extract_word_and_vector_from_glove_file_line(line)
        if file_type == 'word2vec':
            head, vec = extract_word_and_vector_from_word2vec_file_line(line)
        if head in smaller_vocabulary:
            word_map[head] = vec
            # print head
        else:
            # print("Word " + head + " not present in Glove vectors")
            pass

    return word_map


def prepare_word_embeddings_cache(input_folders_with_csv_files,
                                  output_embeddings_cache_file,
                                  embeddings_file_type='word2vec',
                                  embeddings_file_name='/usr/local/data/GoogleNews-vectors-negative300.bin'):
    """
    This is the main method to prepare a smaller embeddings cache for a limited vocabulary that
    is important for the experiments, as loading the full Glove into memory would be just
    too ineffective.
    :param input_folders_with_csv_files: a list of folders that contain csv files
    :param output_embeddings_cache_file: output file for storing word frequencies and embeddings
    :param embeddings_file_name: file with embeddings; default word2vec txt file
    :param embeddings_file_type: type of embeddings file; default 'word2vec'
    """

    # a bit of defensive programming never hurts :)
    if not isinstance(input_folders_with_csv_files, list):
        raise Exception(
            "input_folders_with_csv_files expected as list but was " + str(type(input_folders_with_csv_files)))

    # First, we want to list all input csv files
    all_folders = []
    for single_folder in input_folders_with_csv_files:
        all_folders.extend(single_folder + x for x in os.listdir(single_folder))

    print('All folders:', all_folders)
    frequencies = load_vocabulary_frequencies(all_folders)

    print(len(frequencies), "vocabulary size loaded")

    word_embedding_map = extract_embeddings_vectors_for_given_words(embeddings_file_name, embeddings_file_type,
                                                                    frequencies)

    print(len(word_embedding_map), "words with embeddings found")

    cPickle.dump((frequencies, word_embedding_map), bz2.BZ2File(output_embeddings_cache_file, 'wb'))

    print("Saved to " + output_embeddings_cache_file)


def load_word_frequencies_and_embeddings(saved_embeddings):
    """
    Loads words frequencies (dict) and embeddings (dict) from pickled bz2 file
    :param saved_embeddings:  pkl.bz2 file
    :return: word_frequencies, word_embedding_map
    """
    (frequencies, word_embedding_map) = cPickle.load(bz2.BZ2File(saved_embeddings, 'rb'))

    return frequencies, word_embedding_map


def dictionary_and_embeddings_to_indices(word_frequencies, embeddings):
    """
    Sort words by frequency (descending), adds offset (3 items), maps word indices to embeddings
    and generate random embeddings for padding, start of sequence, and OOV
    :param word_frequencies: dict (word: frequency)
    :param embeddings: dict (word: numpy embeddings array)
    :return: a tuple: word_to_indices_map (word=string: index=int),
    word_index_to_embeddings_map (index=int: embeddings=np.array)
    """

    # sort word frequencies from the most common ones
    sorted_word_frequencies_keys = sorted(word_frequencies, key=word_frequencies.get, reverse=True)

    word_to_indices_map = dict()
    word_index_to_embeddings_map = dict()

    # offset for all words so their indices don't start with 0
    # 0 is reserved for padding
    # 1 is reserved for start of sequence
    # 2 is reserved for OOV
    offset = 3

    # we also need to initialize embeddings for 0, 1, and 2
    # what is the dimension of embeddings?
    embedding_dimension = len(list(embeddings.values())[0])

    # for padding we will use a zero-vector
    vector_padding = [0.0] * embedding_dimension

    # for start of sequence and OOV we add random vectors
    vector_start_of_sequence = 2 * 0.1 * np.random.rand(embedding_dimension) - 0.1
    vector_oov = 2 * 0.1 * np.random.rand(embedding_dimension) - 0.1

    # and add them to the embeddings map (as the first three values)
    word_index_to_embeddings_map[0] = vector_padding
    word_index_to_embeddings_map[1] = vector_start_of_sequence
    word_index_to_embeddings_map[2] = vector_oov

    # iterate with index
    for idx, word in enumerate(sorted_word_frequencies_keys):
        # print idx, word

        new_index = idx + offset

        # update maps
        word_to_indices_map[word] = new_index

        # if the word from vocabulary doesn't have any known embeddings, we treat it as OOV
        if embeddings.get(word) is not None:
            word_index_to_embeddings_map[new_index] = embeddings.get(word)
        else:
            word_index_to_embeddings_map[new_index] = vector_oov

    return word_to_indices_map, word_index_to_embeddings_map


def load_cached_vocabulary_and_embeddings(serialized_file='vocabulary.embeddings.all.pkl.bz2'):
    """
    This is the main method to be used outside this scripts for experiments. It supposes that
    vocabulary and embeddings has already been extracted and cached, so it loads them, initializes
    correctly the mapping from words to indexes (including OOV, padding, and sequence starts)
    and returns both word frequencies and embeddings
    :param serialized_file: where the data has been cached by 'prepare_word_embeddings_cache'
    :return: a tuple (word frequencies map, embeddings map)
    """
    # load
    print("Loading chached vocabulary and embeddings...")
    freq, embeddings_map = load_word_frequencies_and_embeddings(serialized_file)
    print("Cached vocabulary and embeddings successfully loaded from " + serialized_file)

    # show first entry
    # print(freq.items()[0])
    # print(embeddings_map.items()[0])

    word_to_indices_map, word_index_to_embeddings_map = \
        dictionary_and_embeddings_to_indices(freq, embeddings_map)

    # and check types
    assert isinstance(word_to_indices_map, dict)
    assert isinstance(word_index_to_embeddings_map, dict)
    assert isinstance(list(word_index_to_embeddings_map.keys())[0], int)
    assert isinstance(list(word_index_to_embeddings_map.values())[0], list)
    assert isinstance(list(word_to_indices_map.keys())[0], str)
    assert isinstance(list(word_to_indices_map.values())[0], int)

    return word_to_indices_map, word_index_to_embeddings_map


if __name__ == "__main__":
    prepare_word_embeddings_cache(["data/"],
                                  "/tmp/embeddings_cache_file_word2vec.pkl.bz2",
                                  embeddings_file_name='/home/habi/research/data/GoogleNews-vectors-negative300.txt.gz',
                                  embeddings_file_type='word2vec')


