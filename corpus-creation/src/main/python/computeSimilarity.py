import operator
import random
import sys

import skipthoughts
from sklearn.metrics.pairwise import cosine_similarity

# load global model, takes some 5 minutes...
model = skipthoughts.load_model()


# functions......................................................................................

def get_similarity(text1, text2):
    x = [text1, text2]
    vectors = skipthoughts.encode(model, x)

    # need reshaping to prevent warning from scikit-learn
    a = vectors[0].reshape(1, -1)
    b = vectors[1].reshape(1, -1)

    result_sim = float(cosine_similarity(a, b))

    print(result_sim, text1, text2)

    return result_sim


def find_most_dissimilar_reasons(map_reason_gist_param):
    """
    returns a map of {reasonId: reasonId} for which all are most dissimilar
    :param map_reason_gist_param: map reasonId: gist
    :return: map
    """
    result = dict()

    # storing similarities
    similarities_reason_reason_map = dict()

    # initialize it first with None
    for i in map_reason_gist_param:
        similarities_reason_reason_map[i] = dict()
        for j in map_reason_gist_param:
            similarities_reason_reason_map[i][j] = None

    for reason_id1 in map_reason_gist_param:
        for reason_id2 in map_reason_gist_param:
            if similarities_reason_reason_map[reason_id1][reason_id2] is None:
                text1 = map_reason_gist_param[reason_id1]
                text2 = map_reason_gist_param[reason_id2]

                similarity = get_similarity(text1, text2)

                # update the matrix (is symmetrical)
                similarities_reason_reason_map[reason_id1][reason_id2] = similarity
                similarities_reason_reason_map[reason_id2][reason_id1] = similarity
            else:
                print("Similarity ", reason_id1, reason_id2, "computed already")

    for reason_id1 in similarities_reason_reason_map:
        similarity_scores_for_reason_id1 = similarities_reason_reason_map[reason_id1]
        # find minimal similarity - returns an list of tuples
        sorted_by_value = sorted(similarity_scores_for_reason_id1.items(),
                                 key=operator.itemgetter(1))
        # get the lowest value (first key)
        dissimilar_reason_id = sorted_by_value[0][0]

        # add to the result - both because it is symmetrical
        result[reason_id1] = dissimilar_reason_id

    print(result)

    return result


def get_random_reasons(stance_param, map_reason_gist_param, map_stance_reason_gist_param):
    """
    Get a random reasonId from other stances than the current one
    :param stance_param: current stance (we don't want to draw from this one)
    :param map_reason_gist_param: current stance and reasons
    :param map_stance_reason_gist_param: map of all stances and reasons
    """
    result = dict()
    list_copy = list(map_stance_reason_gist_param.keys())

    for current_reason_id in map_reason_gist_param:
        # remove the current stance
        list_copy[:] = [s for s in list_copy if s != stance_param]

        # draw a random stance
        random_stance = random.choice(list_copy)

        random_id = random.choice(list(map_stance_reason_gist_param[random_stance].keys()))

        result[current_reason_id] = random_id

    print("returning random pairs", result)

    return result


# body............................................................................................

def save_result_to_file(dissimilar_reasons_map_param, output_file_name):
    print(dissimilar_reasons_map_param)

    with open(output_file_name, 'w') as f_out:
        for i, j in dissimilar_reasons_map_param.iteritems():
            f_out.write(i + "\t" + j + "\n")
    f_out.close()
    print("Saved to", output_file_name)


def main():
    # use the same pseudo-generator
    random.seed(1234)

    filename = sys.argv[
        1]  # 'mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons-exported-gist.csv'
    filename_out = sys.argv[
        2]  # 'mturk/annotation-task/data/61-reason-disambiguation-batch-0001-5000-4235reasons-dissimilar-reasons.csv'

    f = open(filename, 'r')

    # stance: {reasonId: gist}
    map_stance_reason_gist = {}

    for row in f:
        tabs = row.strip().split('\t')
        stance = tabs[0]
        reason_id = tabs[1]
        gist = tabs[2]

        # add new stance
        if stance not in map_stance_reason_gist:
            map_stance_reason_gist[stance] = dict()

        map_stance_reason_gist[stance][reason_id] = gist

    # show map
    print(len(map_stance_reason_gist), "stances")

    frequencies = dict()

    for stance in map_stance_reason_gist:
        map_reason_gist = map_stance_reason_gist[stance]
        length = len(map_reason_gist)
        frequencies[length] = frequencies.get(length, 0) + 1

    print(frequencies)

    # for results
    dissimilar_reasons_map = dict()

    for stance in map_stance_reason_gist:
        map_reason_gist = map_stance_reason_gist[stance]
        length = len(map_reason_gist)
        if length > 1:
            dissimilar_reasons_map.update(find_most_dissimilar_reasons(map_reason_gist))
        else:
            dissimilar_reasons_map.update(
                get_random_reasons(stance, map_reason_gist, map_stance_reason_gist))
        # save the results in each step...
        save_result_to_file(dissimilar_reasons_map, filename_out)

    pass


# main
if __name__ == '__main__':
    main()
